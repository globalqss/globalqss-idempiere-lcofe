/***********************************************************************
 * This file is part of Adempiere ERP Bazaar                           *
 * http://www.adempiere.org                                            *
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
 *                                                                     *
 * Contributors:                                                       *
 * - Jesus Garcia - GlobalQSS Colombia                                 *
 * - Carlos Ruiz  - GlobalQSS Colombia                                 *
 **********************************************************************/
package org.globalqss.process.fe;


import java.util.List;
import java.util.logging.Level;

import org.adempiere.base.Service;
import org.adempiere.base.ServiceQuery;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.globalqss.model.ILCO_FE_ProcessInvoice;
import org.globalqss.model.MLCOFEAuthorization;


/**
 *	Generate EMail Authorizations
 *	
 *  @author GlobalQSS/jjgq
 */
public class EmailAuthorization extends SvrProcess {

	/** Authorization					*/
	private int			p_LCO_FE_Authorization_ID = 0;

	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("LCO_FE_Authorization_ID"))
				p_LCO_FE_Authorization_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		if (p_LCO_FE_Authorization_ID == 0)
			p_LCO_FE_Authorization_ID = getRecord_ID();

	}	//	prepare

	/**
	 * 	Generate Invoices
	 *	@return info
	 *	@throws Exception
	 */
	protected String doIt () throws Exception
	{
		ServiceQuery query = new ServiceQuery();
		String techProvider = MSysConfig.getValue("QSS_LCO_ProveedorTecnologico", "dian21", getAD_Client_ID());
		query.put("proveedor", techProvider); // QSS_LCO_ProveedorTecnologico
		ILCO_FE_ProcessInvoice custom = Service.locator().locate(ILCO_FE_ProcessInvoice.class, query).getService();
		if (custom == null)
			throw new AdempiereException("No ILCO_FE_ProcessInvoice provider found for technological provider " + techProvider);

		log.info("LCO_FE_Authorization_ID=" + p_LCO_FE_Authorization_ID);
		//
		StringBuilder where = new StringBuilder("LCO_FE_DateAuthorization IS NOT NULL AND IsMailSend='N' AND (Processed='Y' OR LCO_FE_OFE_Contingency_ID IS NOT NULL)");
		if (p_LCO_FE_Authorization_ID != 0)
			where.append(" AND LCO_FE_Authorization_ID=?");
		Query queryAuths = new Query(getCtx(), MLCOFEAuthorization.Table_Name, where.toString(), get_TrxName())
				.setOnlyActiveRecords(true)
				.setClient_ID();
		if (p_LCO_FE_Authorization_ID != 0)
			queryAuths.setParameters(p_LCO_FE_Authorization_ID);
		List<MLCOFEAuthorization> auths = queryAuths.list();

		String msg = custom.sendEMail(auths);
		return msg;
	}	//	doIt


}	//	EmailAuthorization

