/***********************************************************************
 * This file is part of iDempiere ERP Open Source                      *
 * http://www.idempiere.org                                            *
 *                                                                     *
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * You should have received a copy of the GNU General Public License   *
 * along with this program; if not, write to the Free Software         *
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
 * MA 02110-1301, USA.                                                 *
 **********************************************************************/

package org.globalqss.dian.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MMailText;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Msg;
import org.globalqss.model.MLCOFEAuthorization;
import org.globalqss.model.X_LCO_FE_DIAN_Format;
import org.globalqss.model.X_LCO_FE_DocType;
import org.globalqss.util.LCO_FE_Utils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.helpers.AttributesImpl;

/**
 *	Utils for DIAN LCO FE Mail
 */
public class DIAN21_FE_UtilsMail {

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(DIAN21_FE_UtilsMail.class);

	/** Number of authorizations	*/
	private int			m_created = 0;

	private int			m_mailtext_id = 0;

	private String m_Prefix;
	private String m_DocumentNo;
	private int		m_lco_fe_dian_format_id = 0;
	private boolean m_IsOnTesting;
	private String m_File_Name = "";
	private String m_File_Type;
	private String m_FolderRaiz;

	private String m_File_Name_Xml = null;
	private String m_File_Name_Sign = null;
	private String m_File_Name_Resp = null;
	private String m_File_Name_Cont = null;
	private String m_File_Name_Pdf = null;

	/**
	 * 	Send EMails for authorizations
	 * 	@param auths - list of authorizations
	 *	@return info
	 */
	public String sendEMail(List<MLCOFEAuthorization> auths) {
		for (MLCOFEAuthorization authorization : auths) {

			Properties ctx = authorization.getCtx();
			String trxName = authorization.get_TrxName();

			MInvoice inv = new MInvoice (ctx, authorization.getRecord_ID(), trxName);

			MDocType dt = new MDocType(ctx, inv.getC_DocTypeTarget_ID(), trxName);

			X_LCO_FE_DocType fedt = new X_LCO_FE_DocType (ctx, dt.get_ValueAsInt("LCO_FE_DocType_ID"), trxName);

			m_mailtext_id = LCO_FE_Utils.getMailText(MSysConfig.getValue("QSSLCO_FE_PlantillaEmail", "", inv.getAD_Client_ID(), inv.getAD_Org_ID()), inv.getAD_Client_ID(), inv.getAD_Org_ID());
			if ( m_mailtext_id < 1)
				return ("@NotFound@: @R_MailText_ID@ - " + "QSSLCO_FE_PlantillaEmail");

			String dianshortdoctype = fedt.getDianShortDocType();

			// isSOTrx()
			if (inv.isSOTrx()) {
				if (   LCO_FE_Utils.TIPO_COMPROBANTE_FACTURA.equals(dianshortdoctype)
						|| LCO_FE_Utils.TIPO_COMPROBANTE_NC.equals(dianshortdoctype)
						|| LCO_FE_Utils.TIPO_COMPROBANTE_ND.equals(dianshortdoctype)
						|| LCO_FE_Utils.TIPO_COMPROBANTE_CONTINGENCIA.equals(dianshortdoctype)
						|| LCO_FE_Utils.TIPO_COMPROBANTE_EXPORTACION.equals(dianshortdoctype)
						) {
					lcofeinv_MailInvoiceAuth(authorization);
				} else {
					log.warning(Msg.translate(ctx, "NotAvailable") + " "
							+ Msg.getElement(ctx, X_LCO_FE_DocType.COLUMNNAME_LCO_FE_DocType_ID) + "-"
							+ fedt.getValue() + "-"
							+ Msg.getElement(ctx, X_LCO_FE_DocType.COLUMNNAME_DianShortDocType));
				}
			} else { // !isSOTrx()
				log.warning(Msg.translate(ctx, "NotAvailable") + " "
						+ Msg.getElement(ctx, X_LCO_FE_DocType.COLUMNNAME_LCO_FE_DocType_ID) + "-"
						+ fedt.getValue() + "-"
						+ Msg.getElement(ctx, X_LCO_FE_DocType.COLUMNNAME_DianShortDocType));
			}
		}	//	for all authorizations

		return "@Created@ = " + m_created;
	}	//	generate


	public String lcofeinv_MailInvoiceAuth (MLCOFEAuthorization authorization)
	{

		Properties ctx = authorization.getCtx();
		String trxName = authorization.get_TrxName();
		String msg = null;

		try {

			int c_invoice_id = 0;

			m_IsOnTesting = MSysConfig.getBooleanValue("QSSLCO_FE_EnPruebas", false, Env.getAD_Client_ID(ctx));

			m_FolderRaiz = MSysConfig.getValue("QSSLCO_FE_RutaGeneracionXml", null, Env.getAD_Client_ID(ctx));	// Segun SysConfig + Formato

			if (authorization.getAD_Table_ID() == LCO_FE_Utils.InvoiceTableID && authorization.getRecord_ID() > 0)
				c_invoice_id = authorization.getRecord_ID();

			MInvoice invoice = new MInvoice (ctx, c_invoice_id, trxName);
			// LCO_FE_MInvoice fe_invoice = new LCO_FE_MInvoice (ctx, c_invoice_id, trxName);

			if (!(invoice.getDocStatus().equals("CO") || invoice.getDocStatus().equals("CL"))) {
				msg = (Msg.translate(ctx, "NotValid") + " "
						+ Msg.getElement(ctx, MInvoice.COLUMNNAME_DocumentNo) + "-"
						+ invoice.getDocumentNo() + "-"
						+ Msg.getElement(ctx, MInvoice.COLUMNNAME_DocStatus) + "-"
						+ invoice.getDocStatus());
				log.warning(msg);
				return msg;
			}

			MDocType dt = new MDocType(ctx, invoice.getC_DocTypeTarget_ID(), trxName);

			MMailText mText = new MMailText(ctx, m_mailtext_id, trxName);

			X_LCO_FE_DocType fedt = new X_LCO_FE_DocType (ctx, dt.get_ValueAsInt("LCO_FE_DocType_ID"), trxName);

			m_Prefix = "";
			if (dt.getDefiniteSequence().getPrefix() != null)
				m_Prefix = dt.getDefiniteSequence().getPrefix();

			m_DocumentNo = invoice.getDocumentNo().replace(m_Prefix, "").trim();
			//fe_invoice.set_FE_DocumentNo(m_DocumentNo);

			// Formato
			m_lco_fe_dian_format_id = LCO_FE_Utils.getLcoFeDianFormatID(invoice, fedt.getLCO_FE_DocType_ID());
			X_LCO_FE_DIAN_Format f = new X_LCO_FE_DIAN_Format (ctx, m_lco_fe_dian_format_id, trxName);
			m_File_Type = f.getLCO_FE_EDIType();
			//fe_invoice.set_FE_FileType(m_File_Type);

			// Emisor
			MClient client = new MClient(ctx, invoice.getAD_Client_ID(), trxName);
			MOrgInfo oi = MOrgInfo.get(ctx, invoice.getAD_Org_ID(), trxName);
			MBPartner bpe = new MBPartner(ctx, oi.get_ValueAsInt("C_BPartner_ID"), trxName);

			// Adquiriente
			MBPartner bp = new MBPartner(ctx, invoice.getC_BPartner_ID(), trxName);
			Env.setContext(ctx, "#C_BPartner_Name", bp.getName());

			String xmlFileName = LCO_FE_Utils.constructFileName(bpe.getTaxID(), f.getValue(), m_DocumentNo, "xml", true);

			// Ruta completa del archivo
			m_File_Name = m_FolderRaiz + File.separator + LCO_FE_Utils.FOLDER_COMPROBANTES_FIRMADOS + File.separator + xmlFileName;
			//crea los directorios para los archivos xml
			(new File(m_FolderRaiz + File.separator + LCO_FE_Utils.FOLDER_COMPROBANTES_FIRMADOS + File.separator)).mkdirs();

			// Get containerXml
			log.warning("@LCO_FE_Authorization_ID@ -> " + m_File_Name);
			m_File_Name_Cont = m_File_Name.replace(LCO_FE_Utils.FOLDER_COMPROBANTES_FIRMADOS, LCO_FE_Utils.FOLDER_COMPROBANTES_AUTORIZADOS);
			m_File_Name_Cont = m_File_Name_Cont.replace(".xml", "_container.xml");
			File contfile = LCO_FE_Utils.getFileFromStream(m_File_Name_Cont, authorization.getLCO_FE_Authorization_ID(), m_File_Type, "_container");

			if (contfile == null) {
				// Get resourceXml
				m_File_Name_Xml = m_File_Name.replace(LCO_FE_Utils.FOLDER_COMPROBANTES_FIRMADOS, LCO_FE_Utils.FOLDER_COMPROBANTES_GENERADOS);
				File file = LCO_FE_Utils.getFileFromStream(m_File_Name_Xml, authorization.getLCO_FE_Authorization_ID(), m_File_Type, "None");

				if (file == null)
					throw new AdempiereUserError("@NotFound@: " + m_File_Name_Xml);

				if (file.exists() || file.isFile() || file.canRead())
					m_File_Name_Xml = file.getAbsolutePath();

				// Get signedXml
				m_File_Name_Sign = m_File_Name.replace(".xml", "_signed.xml");
				file = LCO_FE_Utils.getFileFromStream(m_File_Name_Sign, authorization.getLCO_FE_Authorization_ID(), m_File_Type, "_signed");

				if (file == null)
					throw new AdempiereUserError("@NotFound@: " + m_File_Name_Sign);

				if (file.exists() || file.isFile() || file.canRead())
					m_File_Name_Sign = file.getAbsolutePath();

				// Get responseXml
				m_File_Name_Resp = m_File_Name.replace(LCO_FE_Utils.FOLDER_COMPROBANTES_FIRMADOS, LCO_FE_Utils.FOLDER_COMPROBANTES_AUTORIZADOS);
				m_File_Name_Resp = m_File_Name_Resp.replace(".xml", "_response.xml");
				File respfile = LCO_FE_Utils.getFileFromStream(m_File_Name_Resp, authorization.getLCO_FE_Authorization_ID(), m_File_Type, "_response.xml");

				if (respfile == null)
					throw new AdempiereUserError("@NotFound@: " + m_File_Name_Resp);

				if (respfile.exists() || respfile.isFile() || respfile.canRead())
					m_File_Name_Resp = respfile.getAbsolutePath();
			}

			// Generate containerXml
			if (contfile == null) {
				contfile = generateAttachedDocument(authorization.get_ID(), m_File_Name_Xml, m_File_Name_Sign, m_File_Name_Resp, m_File_Name_Cont);
				// Attach Contenedor XML -- Primera vez
				// if (m_IsAttachXml)
				if (contfile != null)
					LCO_FE_Utils.attachFile(ctx, trxName, authorization.getLCO_FE_Authorization_ID(), m_File_Name_Cont, LCO_FE_Utils.RESOURCE_XML);
			}

			if (contfile == null)
				throw new AdempiereUserError("@NotValid@ -> " + m_File_Name_Cont);

			if (contfile.exists() || contfile.isFile() || contfile.canRead())
				m_File_Name_Cont = contfile.getAbsolutePath();

			// Get resourcePdf
			m_File_Name_Pdf = m_File_Name.replace(LCO_FE_Utils.FOLDER_COMPROBANTES_FIRMADOS, LCO_FE_Utils.FOLDER_COMPROBANTES_AUTORIZADOS);
			m_File_Name_Pdf = m_File_Name_Pdf.replace(LCO_FE_Utils.RESOURCE_XML, LCO_FE_Utils.RESOURCE_PDF);
			File pdffile = LCO_FE_Utils.getFileFromStream(m_File_Name_Pdf, authorization.getLCO_FE_Authorization_ID(), LCO_FE_Utils.RESOURCE_PDF, "None");

			if (pdffile == null) {
				File pdfinvoice = invoice.createPDF();
				Files.copy(Paths.get(pdfinvoice.getAbsolutePath()), Paths.get(m_File_Name_Pdf), StandardCopyOption.REPLACE_EXISTING);
				// Attach Comprobante PDF -- Primera vez
				// if (m_IsAttachXml)
				LCO_FE_Utils.attachFile(ctx, trxName, authorization.getLCO_FE_Authorization_ID(), m_File_Name_Pdf, LCO_FE_Utils.RESOURCE_PDF);
				pdffile = new File(m_File_Name_Pdf);
			}

			if (pdffile != null && (pdffile.exists() || pdffile.isFile() || pdffile.canRead()))
				m_File_Name_Pdf = pdffile.getAbsolutePath();

			//
			if (MSysConfig.getBooleanValue("QSSLCO_FE_EnvioXmlAutorizadoBPEmail", false, Env.getAD_Client_ID(ctx)))
			{
				List<File> atts = new ArrayList<File>();
				File attachment = (new File (m_File_Name_Cont));		// 1. Xml Contenedor
				atts.add(attachment);
				if (m_File_Name_Pdf != null) {
					File pdfattachment = (new File (m_File_Name_Pdf));	// 2. Comprobante FE
					atts.add(pdfattachment);
				}	

				if (attachment.exists() || attachment.isFile() || attachment.canRead()) {

					// Set Language when enabled
					Language language = Language.getLoginLanguage();		//	Base Language
					String AD_Language = bp.getAD_Language();
					if (AD_Language != null && client.isMultiLingualDocument())
						language = Language.getLanguage(AD_Language);

					log.warning("@SendEMail@ -> " + m_File_Name_Xml);
					// Enviar Email BPartner XML Autorizado
					MUser to = new MUser(ctx, authorization.getAD_UserMail_ID(), trxName);
					mText.setUser(to);								//	Context
					mText.setBPartner(bp.getC_BPartner_ID());		//	Context
					mText.setLanguage(language.getAD_Language());	//	Context
					mText.setPO(invoice);
					String subject =
							(m_IsOnTesting ? LCO_FE_Utils.NOMBRE_CERTIFICACION + " " : "")	// LCO_FE_Utils.NOMBRE_PRODUCCION
							+ bpe.getTaxID() + "-" + bpe.getName()
							+ " " + fedt.getDianDocTypeCode() + "-" + invoice.getDocumentNo();
					// + " " + mText.getMailHeader();
					// + f.get_ValueAsString("XmlPrintLabel");

					String message = mText.getMailText(true);
					message = Env.parseVariable(message, to, trxName, true);

					int salesRep_ID = 0;
					if (MSysConfig.getBooleanValue("QSSLCO_FE_EnvioXmlAutorizadoSalesRepEmail", false, Env.getAD_Client_ID(ctx)))
						salesRep_ID = invoice.getSalesRep_ID();

					int userFromInvoiceId = MSysConfig.getIntValue("QSSLCO_FE_CopiaXmlAutorizadoUserFEEmail", 0, Env.getAD_Client_ID(ctx));
					if (userFromInvoiceId == 0)
						userFromInvoiceId = invoice.get_ValueAsInt("LCO_FE_UserFrom_ID");

					String replyTo = MSysConfig.getValue("QSSLCO_FE_ReplyToEMail", "", Env.getAD_Client_ID(ctx), invoice.getAD_Org_ID());

					int countMail = 0;
					countMail += LCO_FE_Utils.notifyUsers(ctx, mText, replyTo, userFromInvoiceId, authorization.getAD_UserMail_ID(), salesRep_ID, subject, message, atts);

					m_created = countMail;
					if (countMail == 0)
						log.warning("@RequestActionEMailError@ -> " + m_File_Name);
					else {
						log.info("@RequestActionEMailOK@ " + countMail + " Recipients");
						authorization.setIsMailSend(true);
						authorization.saveEx();
					}
				}
			}

			//
		} catch (Exception e) {
			throw new AdempiereException(e);
		}

		return msg;

	} // lcofeinv_MailInvoiceAuth

	/**
	 * Genera XML AttachedDocument v2.1
	 * @param
	 * @return file
	 */
	public File generateAttachedDocument (int authorization_id, String gen_filename, String signed_filename, String resp_filename, String cont_filename) {

		File contfile = null;
		String strdata = null;

		try	{

			// Abre el archivo XML que se desea leer
			Document doc = null;
			doc = LCO_FE_Utils.getDocument(gen_filename);
			doc.normalizeDocument();

			Document resp_doc = null;
			resp_doc = LCO_FE_Utils.getDocument(resp_filename);
			resp_doc.normalizeDocument();

			NodeList tag = null;

			OutputStream  mmDocStream = null;

			//Stream para el documento xml
			mmDocStream = new FileOutputStream (cont_filename, false);
			StreamResult streamResult_menu = new StreamResult(new OutputStreamWriter(mmDocStream,"UTF-8"));
			SAXTransformerFactory tf_menu = (SAXTransformerFactory) SAXTransformerFactory.newInstance();					
			try {
				tf_menu.setAttribute("indent-number", Integer.valueOf(2));
			} catch (Exception e) {
				// swallow
			}
			TransformerHandler mmDoc = tf_menu.newTransformerHandler();	
			Transformer serializer_menu = mmDoc.getTransformer();	
			serializer_menu.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			// serializer_menu.setOutputProperty(OutputKeys.STANDALONE, "no");
			serializer_menu.setOutputProperty(OutputKeys.INDENT,"yes");
			// serializer_menu.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
			mmDoc.setResult(streamResult_menu);

			mmDoc.startDocument();

			AttributesImpl atts = new AttributesImpl();

			// Encabezado
			atts.clear();
			atts.addAttribute("", "", "xmlns", "CDATA", "urn:oasis:names:specification:ubl:schema:xsd:AttachedDocument-2");
			atts.addAttribute("", "", "xmlns:ds", "CDATA", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");
			atts.addAttribute("", "", "xmlns:cac", "CDATA", "urn:un:unece:uncefact:codelist:specification:54217:2001");
			atts.addAttribute("", "", "xmlns:cbc", "CDATA", "urn:un:unece:uncefact:codelist:specification:66411:2001");
			atts.addAttribute("", "", "xmlns:ccts", "CDATA", "urn:un:unece:uncefact:codelist:specification:IANAMIMEMediaType:2003");
			atts.addAttribute("", "", "xmlns:ext", "CDATA", "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2");
			atts.addAttribute("", "", "xmlns:xades", "CDATA", "urn:oasis:names:specification:ubl:schema:xsd:QualifiedDatatypes-2");
			atts.addAttribute("", "", "xmlns:xades141", "CDATA", "http://uri.etsi.org/01903/v1.4.1#");

			mmDoc.startElement("", "", "AttachedDocument", atts);

			atts.clear();

			tag = doc.getElementsByTagName("cbc:UBLVersionID");
			addHeaderElement(mmDoc, "cbc:UBLVersionID", tag.item(tag.getLength()-1).getTextContent(), atts);	// UBL 2.1
			addHeaderElement(mmDoc, "cbc:CustomizationID", "Documentos adjuntos", atts);
			addHeaderElement(mmDoc, "cbc:ProfileExecutionID", "Factura Electrónica de Venta", atts);
			tag = doc.getElementsByTagName("cbc:ProfileExecutionID");
			addHeaderElement(mmDoc, "cbc:ProfileExecutionID", tag.item(tag.getLength()-1).getTextContent(), atts);
			addHeaderElement(mmDoc, "cbc:ID", String.valueOf(authorization_id), atts);	// Consecutivo propio
			tag = doc.getElementsByTagName("cbc:IssueDate");
			addHeaderElement(mmDoc, "cbc:IssueDate", tag.item(tag.getLength()-1).getTextContent(), atts);
			tag = doc.getElementsByTagName("cbc:IssueTime");
			addHeaderElement(mmDoc, "cbc:IssueTime", tag.item(tag.getLength()-1).getTextContent(), atts);
			addHeaderElement(mmDoc, "cbc:DocumentType", "Contenedor de Factura Electrónica", atts);
			tag = doc.getElementsByTagName("cbc:ID");
			// getTagNodes (tag, "All", "cbc:ID");	// To debug
			addHeaderElement(mmDoc, "ParentDocumentID", tag.item(0).getTextContent(), atts);
			//
			mmDoc.startElement("","","cac:SenderParty", atts);
			mmDoc.startElement("","","cac:PartyTaxScheme", atts);
			tag = doc.getElementsByTagName("cbc:Name");
			addHeaderElement(mmDoc, "cbc:RegistrationName", tag.item(0).getTextContent(), atts);
			tag = doc.getElementsByTagName("cbc:CompanyID");
			atts.clear();
			atts.addAttribute("", "", "schemeAgencyID", "CDATA", getTagAttributes(tag, 0, "schemeAgencyID", false));
			atts.addAttribute("", "", "schemeID", "CDATA", getTagAttributes(tag, 0, "schemeID", false));
			atts.addAttribute("", "", "schemeName", "CDATA", getTagAttributes(tag, 0, "schemeName", false));
			// atts.addAttribute("", "", "all", "CDATA", getTagAttributes(tag, 0, "All"));
			addHeaderElement(mmDoc, "cbc:CompanyID", tag.item(0).getTextContent(), atts);
			tag = doc.getElementsByTagName("cbc:TaxLevelCode");
			atts.clear();
			atts.addAttribute("", "", "listName", "CDATA", getTagAttributes(tag, 0, "listName", false));
			addHeaderElement(mmDoc, "cbc:TaxLevelCode", tag.item(0).getTextContent(), atts);
			atts.clear();
			mmDoc.startElement("","","cac:TaxScheme", atts);
			tag = doc.getElementsByTagName("cbc:ID");	
			addHeaderElement(mmDoc, "cbc:ID", tag.item(4).getTextContent(), atts);
			tag = doc.getElementsByTagName("cbc:Name");
			addHeaderElement(mmDoc, "cbc:Name", tag.item(3).getTextContent(), atts);
			mmDoc.endElement("","","cac:TaxScheme");
			mmDoc.endElement("","","cac:PartyTaxScheme");
			mmDoc.endElement("","","cac:SenderParty");
			//
			mmDoc.startElement("","","cac:ReceiverParty", atts);
			mmDoc.startElement("","","cac:PartyTaxScheme", atts);
			tag = doc.getElementsByTagName("cbc:Name");
			addHeaderElement(mmDoc, "cbc:RegistrationName", tag.item(4).getTextContent(), atts);
			tag = doc.getElementsByTagName("cbc:CompanyID");
			atts.clear();
			atts.addAttribute("", "", "schemeAgencyID", "CDATA", getTagAttributes(tag, 2, "schemeAgencyID", false));
			atts.addAttribute("", "", "schemeID", "CDATA", getTagAttributes(tag, 2, "schemeID", false));
			atts.addAttribute("", "", "schemeName", "CDATA", getTagAttributes(tag, 2, "schemeName", false));
			// atts.addAttribute("", "", "all", "CDATA", getTagAttributes(tag, 2, "All"));
			addHeaderElement(mmDoc, "cbc:CompanyID", tag.item(2).getTextContent(), atts);
			tag = doc.getElementsByTagName("cbc:TaxLevelCode");
			atts.clear();
			atts.addAttribute("", "", "listName", "CDATA", getTagAttributes(tag, 1, "listName", false));
			addHeaderElement(mmDoc, "cbc:TaxLevelCode", tag.item(1).getTextContent(), atts);
			atts.clear();
			mmDoc.startElement("","","cac:TaxScheme", atts);
			tag = doc.getElementsByTagName("cbc:ID");	
			addHeaderElement(mmDoc, "cbc:ID", tag.item(8).getTextContent(), atts);
			tag = doc.getElementsByTagName("cbc:Name");
			addHeaderElement(mmDoc, "cbc:Name", tag.item(6).getTextContent(), atts);
			mmDoc.endElement("","","cac:TaxScheme");
			mmDoc.endElement("","","cac:PartyTaxScheme");
			mmDoc.endElement("","","cac:ReceiverParty");
			//
			mmDoc.startElement("","","cac:Attachment", atts);
			mmDoc.startElement("","","cac:ExternalReference", atts);
			addHeaderElement(mmDoc, "cbc:MimeCode", "text/xml", atts);
			addHeaderElement(mmDoc, "cbc:EncodingCode", "UTF-8", atts);
			mmDoc.startElement("","","cbc:Description", atts);
			mmDoc.startCDATA();
			strdata = Files.readString(Paths.get(signed_filename), StandardCharsets.UTF_8);	
			mmDoc.characters(strdata.toCharArray(),0,strdata.toCharArray().length);
			mmDoc.endCDATA();
			mmDoc.endElement("","","cbc:Description");
			mmDoc.endElement("","","cac:ExternalReference");
			mmDoc.endElement("","","cac:Attachment");
			//
			mmDoc.startElement("","","cac:ParentDocumentLineReference", atts);
			addHeaderElement(mmDoc, "cbc:LineID", "1", atts);
			mmDoc.startElement("","","cac:DocumentReference", atts);
			tag = doc.getElementsByTagName("cbc:ID");
			addHeaderElement(mmDoc, "cbc:ID", tag.item(0).getTextContent(), atts);
			tag = doc.getElementsByTagName("cbc:UUID");
			atts.clear();
			atts.addAttribute("", "", "schemeName", "CDATA", getTagAttributes(tag, 0, "schemeName", false));
			addHeaderElement(mmDoc, "cbc:UUID", tag.item(tag.getLength()-1).getTextContent(), atts);
			atts.clear();
			tag = doc.getElementsByTagName("cbc:IssueDate");
			addHeaderElement(mmDoc, "cbc:IssueDate", tag.item(tag.getLength()-1).getTextContent(), atts);
			addHeaderElement(mmDoc, "cbc:DocumentType", "ApplicationResponse", atts);
			//
			mmDoc.startElement("","","cac:Attachment", atts);
			mmDoc.startElement("","","cac:ExternalReference", atts);
			addHeaderElement(mmDoc, "cbc:MimeCode", "text/xml", atts);
			addHeaderElement(mmDoc, "cbc:EncodingCode", "UTF-8", atts);
			mmDoc.startElement("","","cbc:Description", atts);
			mmDoc.startCDATA();
			strdata = Files.readString(Paths.get(resp_filename), StandardCharsets.UTF_8);	
			mmDoc.characters(strdata.toCharArray(),0,strdata.toCharArray().length);
			mmDoc.endCDATA();
			mmDoc.endElement("","","cbc:Description");
			mmDoc.endElement("","","cac:ExternalReference");
			mmDoc.endElement("","","cac:Attachment");
			//
			mmDoc.startElement("","","cac:ResultOfVerification", atts);
			addHeaderElement(mmDoc, "cbc:ValidatorID", "Unidad Especial Dirección de Impuestos y Aduanas Nacionales", atts);
			tag = resp_doc.getElementsByTagName("cbc:ResponseCode");
			if (tag == null || tag.item(0) == null)
				tag = resp_doc.getElementsByTagName("b:StatusCode");
			addHeaderElement(mmDoc, "cbc:ValidationResultCode", tag.item(0).getTextContent(), atts);
			tag = resp_doc.getElementsByTagName("xades:SigningTime");
			if (tag == null || tag.getLength() == 0)
				tag = resp_doc.getElementsByTagName("u:Created"); 
			addHeaderElement(mmDoc, "cbc:ValidationDate", tag.item(tag.getLength()-1).getTextContent().substring(0, 10), atts);
			addHeaderElement(mmDoc, "cbc:ValidationTime", tag.item(tag.getLength()-1).getTextContent().substring(11), atts);
			mmDoc.endElement("","","cac:ResultOfVerification");
			mmDoc.endElement("","","cac:DocumentReference");
			mmDoc.endElement("","","cac:ParentDocumentLineReference");

			mmDoc.endElement("","","AttachedDocument");

			mmDoc.endDocument();

			if (mmDocStream != null) {
				try {
					mmDocStream.close();
				} catch (Exception e2) {}
			}

			contfile = (new File (cont_filename));

		} catch (Exception e) {
			throw new AdempiereException(cont_filename + " " + e.getLocalizedMessage());
		}

		return contfile;

	}	// generateAttachedDocument

	// TODO Use DIAN21_FE_UtilsXML.addHeaderElement
	public void addHeaderElement(TransformerHandler mmDoc, String att, String value, AttributesImpl atts) throws Exception {
		if (att != null) {
			mmDoc.startElement("","",att,atts);
			mmDoc.characters(value.toCharArray(),0,value.toCharArray().length);
			mmDoc.endElement("","",att);
		} else {
			throw new AdempiereUserError(att + " empty");
		}
	}

	/**
	 * 	void getTagNodes
	 *  Dummy
	 *  @param
	 * 	@return void
	 */
	public void getTagNodes(NodeList tag, String parentnode, String nodename) {

		String nodelist = "";

		for ( int i=0; i < tag.getLength();  i++)
			if (parentnode.equals("All") || parentnode.equals(tag.item(i).getParentNode().getNodeName())) {
				nodelist = nodelist + i + "-"
						// + tag.item(i).getParentNode().getParentNode().getNodeName()	+ "-"	// GrandF
						+ tag.item(i).getParentNode().getNodeName()							// Father
						+ "<" + nodename + ">" + tag.item(i).getTextContent() + "</"+ nodename + ">" + "\r\n";
			}
		log.warning("@Warning@ -> " + nodelist);

		// return i;
		// return nodelist;
	}

	/**
	 * 	String getTagAttributes
	 *  if nodename="All" returns full list
	 *  @param
	 * 	@return taglist
	 */
	public String getTagAttributes(NodeList tag, int index, String nodename, boolean isprintname) {

		String taglist = "";

		for ( int i=0; i < tag.item(index).getAttributes().getLength();  i++)
			if (nodename.equals("All") || nodename.equals(tag.item(index).getAttributes().item(i).getNodeName())) {
				if (isprintname)
					taglist = taglist + " " + tag.item(index).getAttributes().item(i).getNodeName() + "=\"";
				taglist = taglist + tag.item(index).getAttributes().item(i).getNodeValue();
				if (isprintname)
					taglist = taglist + "\"";
			}

		return taglist;
	}

}	// DIAN21_FE_UtilsMail
