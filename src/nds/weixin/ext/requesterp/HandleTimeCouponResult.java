package nds.weixin.ext.requesterp;

import java.sql.Connection;

import nds.query.QueryEngine;
import nds.query.QueryException;

public class HandleTimeCouponResult implements HandleSendCouponResult{
	private static final String DO_SUCCESS_SQL = "insert into wx_timecoupons_success (ID, AD_CLIENT_ID, AD_ORG_ID, WX_TIMECOUPON_ID, WX_VIP_ID, OWNERID, MODIFIERID, CREATIONDATE, MODIFIEDDATE, ISACTIVE)  select get_sequences('WX_TIMECOUPONS_SUCCESS'), AD_CLIENT_ID, AD_ORG_ID,ID,?, OWNERID, MODIFIERID, sysdate, sysdate, ISACTIVE from wx_timecoupons  where id=?";
 
	@Override
	public void doSuccess(int vipid, int sendCouponid, QueryEngine qe, Connection con) throws QueryException {
		qe.executeUpdate(DO_SUCCESS_SQL, new Object[]{vipid,sendCouponid}, con);
	}

	@Override
	public void doFail(int vipid, int sendCouponid, QueryEngine qe, Connection con) {
		
	}
	
}
