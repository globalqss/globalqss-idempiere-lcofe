package org.globalqss.dian.util;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MSequence;
import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.globalqss.model.LCO_FE_MInvoice;
import org.globalqss.model.X_LCO_FE_Authorization;
import org.globalqss.model.X_LCO_FE_DIAN_Format;
import org.globalqss.util.LCO_FE_Utils;
import org.w3c.dom.Document;

/**
 *	Utils for LCO FE Xml
 *
 *  @author Jesus Garcia - globalqss - Quality Systems & Solutions - http://globalqss.com
 *	@version $Id: LCO_FE_UtilsXml21.java,v 1.0 2013/05/27 23:01:26 cruiz Exp $
 */

public class DIAN21_FE_UtilsWS_PHP {

	/**	s_log			*/
	private static CLogger s_log = CLogger.getCLogger(DIAN21_FE_UtilsWS_PHP.class);

	private static String m_Output_Directory;
	private static String m_Resource_To_Send;

	public String sendFile(LCO_FE_MInvoice invoice) {
		String msg = null;

		m_Output_Directory = invoice.get_FE_FolderRaiz() + File.separator + LCO_FE_Utils.FOLDER_COMPROBANTES_TRANSMITIDOS;
		(new File(invoice.get_FE_FolderRaiz() + File.separator + LCO_FE_Utils.FOLDER_COMPROBANTES_TRANSMITIDOS + File.separator)).mkdirs();

		if (!invoice.is_FE_UseContingency() && !invoice.is_FE_GenerateInBatch()) {

			m_Resource_To_Send = invoice.get_FE_FileToSend();
			// Procesar Recepcion PTFE -- WS EnvioFacturaElectronica
			s_log.warning("@Sending@ -> " + m_Resource_To_Send);
			String pathScripts = MSysConfig.getValue("QSSLCO_FE_Path_PHP_Scripts", "", Env.getAD_Client_ID(Env.getCtx()));
			if (Util.isEmpty(pathScripts)) {
				throw new AdempiereException("Please configure SysConfig QSSLCO_FE_Path_PHP_Scripts");
			}
			String sendScript = MSysConfig.getValue("QSSLCO_FE_SendScript", "RequestSendBillSync.php", Env.getAD_Client_ID(Env.getCtx()));
			String suffix = sendScript.substring(7, sendScript.length()-4);

			String output_base_file = m_Output_Directory 
					+ File.separator 
					+ m_Resource_To_Send.substring(m_Resource_To_Send.lastIndexOf("/") + 1, m_Resource_To_Send.length() - 4)
					+ "_" + suffix;

			boolean isOnTesting = MSysConfig.getBooleanValue("QSSLCO_FE_EnPruebas", false, Env.getAD_Client_ID(Env.getCtx()));
			String[] command = new String[] {
					"php",
					pathScripts + File.separator + sendScript,
					Integer.toString(invoice.getAD_Client_ID()),
					isOnTesting ? LCO_FE_Utils.AMBIENTE_CERTIFICACION : LCO_FE_Utils.AMBIENTE_PRODUCCION,
					output_base_file,
					invoice.get_FE_FileToSend()
			};

			msg = LCO_FE_Utils.runCommand(command);

			if ("OK".equals(msg)) {
				msg = null;
				// out of trx
				X_LCO_FE_Authorization a = new X_LCO_FE_Authorization (invoice.getCtx(), invoice.get_ValueAsInt("LCO_FE_Authorization_ID"), null);
				if (invoice.is_FE_AttachXml()) {
					LCO_FE_Utils.attachFile(a.getCtx(), a.get_TrxName(), a.getLCO_FE_Authorization_ID(), output_base_file + "_request.xml", LCO_FE_Utils.RESOURCE_XML);
					LCO_FE_Utils.attachFile(a.getCtx(), a.get_TrxName(), a.getLCO_FE_Authorization_ID(), output_base_file + "_answer.xml", LCO_FE_Utils.RESOURCE_XML);
				}
				if ("RequestSendBillSync.php".equals(sendScript)) {
					setErrorMsgFromFile_PHP(a, output_base_file + "_answer.xml");
					if (! a.isProcessed()) {
						msg = a.getErrorMsg();
					}
				}
			}
		}

		return msg;
	}

	public String getStatus(X_LCO_FE_Authorization auth, boolean force) {
		
		String msg = null;
		
		if (auth.isProcessed() && !force)
			return "@DocProcessed@";

		String pathScripts = MSysConfig.getValue("QSSLCO_FE_Path_PHP_Scripts", "", Env.getAD_Client_ID(Env.getCtx()));
		if (Util.isEmpty(pathScripts)) {
			throw new AdempiereException("Please configure SysConfig QSSLCO_FE_Path_PHP_Scripts");
		}
		
		String folderRaiz = MSysConfig.getValue("QSSLCO_FE_RutaGeneracionXml", null, Env.getAD_Client_ID(Env.getCtx()));	// Segun SysConfig + Formato
		m_Output_Directory = folderRaiz + File.separator + LCO_FE_Utils.FOLDER_COMPROBANTES_AUTORIZADOS;
		(new File(folderRaiz + File.separator + LCO_FE_Utils.FOLDER_COMPROBANTES_AUTORIZADOS + File.separator)).mkdirs();
		
		MInvoice invoice = new MInvoice (auth.getCtx(), auth.getRecord_ID(), auth.get_TrxName());
		String  documentno = invoice.getDocumentNo().trim();
		if (! invoice.isProcessed()) {
			documentno = auth.getValue().trim().substring(auth.getValue().lastIndexOf("-")+1);
		}
		MDocType dt = MDocType.get(auth.getCtx(), invoice.getC_DocTypeTarget_ID());
		MSequence seq = new MSequence(auth.getCtx(), dt.getDefiniteSequence_ID(), auth.get_TrxName());
		String prefix = seq.getPrefix();
		documentno = (prefix != null ? documentno.replace(prefix, "") : documentno);
		
		// Emisor
		MOrgInfo oi = MOrgInfo.get(auth.getCtx(), invoice.getAD_Org_ID(), auth.get_TrxName());
		MBPartner bpe = new MBPartner(auth.getCtx(), oi.get_ValueAsInt("C_BPartner_ID"), auth.get_TrxName());
		
		// Formato
		X_LCO_FE_DIAN_Format df = new X_LCO_FE_DIAN_Format (auth.getCtx(), auth.getLCO_FE_DIAN_Format_ID(), auth.get_TrxName());
		
		String xmlFileName = LCO_FE_Utils.constructFileName(bpe.getTaxID(), df.getValue(), documentno, df.getLCO_FE_EDIType(), true);
		
		String output_base_file = m_Output_Directory 
				+ File.separator
				+ xmlFileName.substring(0, xmlFileName.lastIndexOf("."))
				+ "_GetStatus";
		output_base_file = output_base_file.replace( "face_", "ws_");

		String statusScript = MSysConfig.getValue("QSSLCO_FE_StatusScript", "RequestGetStatus.php", Env.getAD_Client_ID(Env.getCtx()));
		
		boolean isOnTesting = MSysConfig.getBooleanValue("QSSLCO_FE_EnPruebas", false, Env.getAD_Client_ID(Env.getCtx()));
		String[] command = new String[] {
				"php",
				pathScripts + File.separator + statusScript,
				Integer.toString(auth.getAD_Client_ID()),
				isOnTesting ? LCO_FE_Utils.AMBIENTE_CERTIFICACION : LCO_FE_Utils.AMBIENTE_PRODUCCION,
				output_base_file,
				auth.getLCO_FE_Dian_Uuid()
		};

		msg = LCO_FE_Utils.runCommand(command);

		if ("OK".equals(msg)) {
			if (MSysConfig.getBooleanValue("QSSLCO_FE_DebugEnvioRecepcion", true, Env.getAD_Client_ID(Env.getCtx()))) {
				LCO_FE_Utils.attachFile(auth.getCtx(), auth.get_TrxName(), auth.getLCO_FE_Authorization_ID(), output_base_file + "_request.xml", LCO_FE_Utils.RESOURCE_XML);
				LCO_FE_Utils.attachFile(auth.getCtx(), auth.get_TrxName(), auth.getLCO_FE_Authorization_ID(), output_base_file + "_answer.xml", LCO_FE_Utils.RESOURCE_XML);
			}
			setErrorMsgFromFile_PHP(auth, output_base_file + "_answer.xml");
		}

		return msg;
	}

	public String getNumberingRange(MOrg org) {

		String msg = null;

		String pathScripts = MSysConfig.getValue("QSSLCO_FE_Path_PHP_Scripts", "", Env.getAD_Client_ID(Env.getCtx()));
		if (Util.isEmpty(pathScripts)) {
			throw new AdempiereException("Please configure SysConfig QSSLCO_FE_Path_PHP_Scripts");
		}

		String folderRaiz = MSysConfig.getValue("QSSLCO_FE_RutaGeneracionXml", null, Env.getAD_Client_ID(Env.getCtx()));	// Segun SysConfig + Formato
		m_Output_Directory = folderRaiz + File.separator + LCO_FE_Utils.FOLDER_COMPROBANTES_TRANSMITIDOS;
		(new File(folderRaiz + File.separator + LCO_FE_Utils.FOLDER_COMPROBANTES_TRANSMITIDOS + File.separator)).mkdirs();

		// Emisor
		MOrgInfo oi = org.getInfo();

		String output_base_file = m_Output_Directory 
				+ File.separator
				+ "GetNumberingRange"
				+ new SimpleDateFormat("yyyyMMddHHmm").format(new Date());

		String numberingRangeScript = MSysConfig.getValue("QSSLCO_FE_NumberingRangeScript", "RequestGetNumberingRange.php", Env.getAD_Client_ID(Env.getCtx()));

		boolean isOnTesting = MSysConfig.getBooleanValue("QSSLCO_FE_EnPruebas", false, Env.getAD_Client_ID(Env.getCtx()));
		String[] command = new String[] {
				"php",
				pathScripts + File.separator + numberingRangeScript,
				Integer.toString(org.getAD_Client_ID()),
				isOnTesting ? LCO_FE_Utils.AMBIENTE_CERTIFICACION : LCO_FE_Utils.AMBIENTE_PRODUCCION,
				output_base_file,
				oi.get_ValueAsString("LCO_FE_IdCompany"),
				oi.get_ValueAsString("LCO_FE_IdCompany"),
				oi.get_ValueAsString("LCO_FE_UserName")
		};

		msg = LCO_FE_Utils.runCommand(command);

		if ("OK".equals(msg)) {
			if (MSysConfig.getBooleanValue("QSSLCO_FE_DebugEnvioRecepcion", true, Env.getAD_Client_ID(Env.getCtx()))) {
				LCO_FE_Utils.attachFileToOrg(org, output_base_file + "_request.xml", LCO_FE_Utils.RESOURCE_XML);
				LCO_FE_Utils.attachFileToOrg(org, output_base_file + "_answer.xml", LCO_FE_Utils.RESOURCE_XML);
			}
			// TODO: create LCO_PrintedFormControl records based on the answer
		}

		return msg;
	}

	/**
	 * 	void setErrorMsgFromFile_PHP
	 *  @param invoice Invoice
	 * 	@return void
	 */
	public static void setErrorMsgFromFile_PHP(X_LCO_FE_Authorization auth, String file) {
		try {
			Document document = LCO_FE_Utils.getDocument(file);
	        String validationDate = LCO_FE_Utils.evaluateXPath(document, "//*[local-name()='Created']/text()").get(0);
	        String isValid = LCO_FE_Utils.evaluateXPath(document, "//*[local-name()='IsValid']/text()").get(0);
	        String statusCode = "";
	        try {
		        statusCode = LCO_FE_Utils.evaluateXPath(document, "//*[local-name()='StatusCode']/text()").get(0);
	        } catch (IndexOutOfBoundsException e) {}
	        String statusDescription = "";
	        try {
		        statusDescription = LCO_FE_Utils.evaluateXPath(document, "//*[local-name()='StatusDescription']/text()").get(0);
	        } catch (IndexOutOfBoundsException e) {}
	        String statusMessage = "";
	        try {
	        	statusMessage = LCO_FE_Utils.evaluateXPath(document, "//*[local-name()='StatusMessage']/text()").get(0);
	        } catch (IndexOutOfBoundsException e) {}
	        List<String >errors = LCO_FE_Utils.evaluateXPath(document, "//*[local-name()='ErrorMessage']/*/text()");
	        StringBuilder errorMsg = new StringBuilder();
	        errorMsg.append(isValid).append(" ").append(statusCode).append(" ").append(statusDescription).append("\n").append(statusMessage);
	        for (String error : errors) {
	        	errorMsg.append("\n").append(error);
	        }
	        auth.setErrorMsg(errorMsg.toString());
	        BigDecimal statusCodeBD = Env.ONE.negate();
	        try {
		        statusCodeBD = new BigDecimal(statusCode);
	        } catch (NumberFormatException e) {}
	        auth.setLCO_FE_IdErrorCode(statusCodeBD);
        	String output_Directory = file.substring(0, file.lastIndexOf(File.separator));
        	String file_response = null;
	        if (LCO_FE_Utils.STATUS_CODE_PROCESADO.equals(statusCode) && "true".equals(isValid)) {
	        	if (auth.getLCO_FE_DateAuthorization() == null) {
	        		auth.setLCO_FE_DateAuthorization(LCO_FE_Utils.fixTimeZone(validationDate).toString());	// First Time
	        	} 
	        	auth.setProcessed(true);
	        	auth.saveEx();
	        	output_Directory = output_Directory.replace(LCO_FE_Utils.FOLDER_COMPROBANTES_TRANSMITIDOS, LCO_FE_Utils.FOLDER_COMPROBANTES_AUTORIZADOS);
	        } else {
	        	output_Directory = output_Directory.replace(LCO_FE_Utils.FOLDER_COMPROBANTES_TRANSMITIDOS, LCO_FE_Utils.FOLDER_COMPROBANTES_RECHAZADOS);
	        }
	        (new File(output_Directory)).mkdirs();
	        file_response = output_Directory + File.separator + file.substring(file.lastIndexOf(File.separator) + 1, file.lastIndexOf(File.separator) + 25) + "_response" + "." + LCO_FE_Utils.RESOURCE_XML;
	        file_response = file_response.replace("ws_", "face_");
	        Files.copy(Paths.get(file), Paths.get(file_response), StandardCopyOption.REPLACE_EXISTING);
	        if (auth.isProcessed())	// TODO Reviewme
	        	LCO_FE_Utils.attachFile(auth.getCtx(), auth.get_TrxName(), auth.getLCO_FE_Authorization_ID(), file_response, LCO_FE_Utils.RESOURCE_XML);
	        auth.saveEx();
		} catch (Exception e) {
			throw new AdempiereException(e);
		}
	}

}	// DIAN21_FE_UtilsWS_PHP
