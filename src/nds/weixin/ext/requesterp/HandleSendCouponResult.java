package nds.weixin.ext.requesterp;

import java.sql.Connection;

import nds.query.QueryEngine;
import nds.query.QueryException;

public interface HandleSendCouponResult {
	void doSuccess(int vipid,int sendCouponid,QueryEngine qe,Connection con) throws QueryException;
	void doFail(int vipid,int sendCouponid,QueryEngine qe,Connection con) throws QueryException;
}
