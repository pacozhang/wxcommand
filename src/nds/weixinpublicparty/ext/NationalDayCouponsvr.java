package nds.weixinpublicparty.ext;

import nds.control.event.DefaultWebEvent;
import nds.control.util.ValueHolder;
import nds.control.web.ClientControllerWebImpl;
import nds.control.web.WebUtils;
import nds.process.SvrProcess;
import nds.weixinpublicparty.ext.common.CommonSendCoupon;

public class NationalDayCouponsvr extends SvrProcess {

	@Override
	protected void prepare() {
		// TODO Auto-generated method stub

	}

	@Override
	protected String doIt() throws Exception {
		// TODO Auto-generated method stub
		ValueHolder vh=null;
		String couponsql="select fc.ad_client_id,fc.id,cp.num,nvl(cp.usetype1,2),nvl(cp.value,'0'),to_char(nvl(cp.starttime,sysdate), 'YYYYMMDD'),to_char(decode(nvl(cp.validay,0),0, nvl(cp.endtime, add_months(sysdate,1)),nvl(cp.starttime,sysdate)+nvl(cp.validay,0)), 'YYYYMMDD')"
				+",get_fitler_sql(fc.releasevips) from wx_festivalcoupon fc join WX_COUPON cp on cp.id=fc.issuingcoupons"
				+" where nvl(cp.isactive, 'N') = 'Y'"
				+" and fc.releasetype = 'G'"
				+" and nvl(fc.status,'1') = '2'"
				+" and nvl(fc.whethernot,'Y') = 'N'"
				+" and to_char(sysdate,'YYYYMMDD')<to_char(decode(nvl(cp.validay,0),0, nvl(cp.endtime, add_months(sysdate,1)),nvl(cp.starttime,sysdate)+nvl(cp.validay,0)), 'YYYYMMDD')";
    //查出要发的会员
	String vipsql="select v.id,v.wechatno,v.vipcardno"
		   +" from   wx_vip v"
		   +" where  v.ad_client_id=?"
		   +" and v.id ";
	//更新是否发放
	String updatesql="update wx_festivalcoupon fc set fc.whethernot='Y' where fc.id=?";
	vh = CommonSendCoupon.sendCoupon(couponsql,vipsql,updatesql,log);
/*		try{
			ClientControllerWebImpl controller=(ClientControllerWebImpl)WebUtils.getServletContextManager().getActor(nds.util.WebKeys.WEB_CONTROLLER);
			DefaultWebEvent event=new DefaultWebEvent("CommandEvent");

			event.setParameter("command", "nds.weixinpublicparty.ext.NationalDayCommand");
			vh= controller.handleEvent(event);
		}catch(Exception e){
			log.debug("call nds.weixinpublicparty.ext.NationalDayCommand error->"+e.getLocalizedMessage());
			e.printStackTrace();
		}*/
		return null;
	}

}
