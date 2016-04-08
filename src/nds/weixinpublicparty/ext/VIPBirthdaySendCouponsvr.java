package nds.weixinpublicparty.ext;

import nds.control.event.DefaultWebEvent;
import nds.control.util.ValueHolder;
import nds.control.web.ClientControllerWebImpl;
import nds.control.web.WebUtils;
import nds.process.SvrProcess;
import nds.weixinpublicparty.ext.common.CommonSendCoupon;

public class VIPBirthdaySendCouponsvr extends SvrProcess{

	@Override
	protected void prepare() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected String doIt() throws Exception {
		ValueHolder vh=null;
/*		String couponsql="select bc.ad_client_id,bc.id,cp.num,nvl(cp.usetype1,2),nvl(cp.value,'0'),to_char(nvl(cp.starttime,sysdate), 'YYYYMMDD'),to_char(decode(nvl(cp.validay,0),0, nvl(cp.endtime, add_months(sysdate,1)),nvl(cp.starttime,sysdate)+nvl(cp.validay,0)), 'YYYYMMDD')"
				+" from wx_birthdaycoupon bc join WX_COUPON cp on cp.id=bc.coupon"
				+" where nvl(cp.isactive, 'N') = 'Y'"
				+" and nvl(bc.status,'1') = '2'"
				+" and nvl(bc.whethernot,'Y') = 'N'"
				+" and to_number(to_char(to_date(bc.senddate,'yyyymmdd'),'yyyymm')) = to_number(to_char(sysdate,'yyyymm'))"
				+" and to_char(sysdate,'YYYYMMDD')<to_char(decode(nvl(cp.validay,0),0, nvl(cp.endtime, add_months(sysdate,1)),nvl(cp.starttime,sysdate)+nvl(cp.validay,0)), 'YYYYMMDD')";
		
				
		
		String vipsql="select v.id,v.wechatno,v.vipcardno"
			   +" from   wx_vip v"
			   +" where  v.ad_client_id=?"
			   +" and 	 length(trim(v.birthday))=8"
			   +" and    to_number(to_char(to_date(v.birthday,'yyyymmdd'),'mm')) = to_number(to_char(sysdate,'mm'))";
		
		String updatesql="update wx_birthdaycoupon bc set bc.whethernot='Y' where bc.id=?";
		vh = CommonSendCoupon.sendCoupon(couponsql,vipsql,updatesql,log);*/
		try{
			ClientControllerWebImpl controller=(ClientControllerWebImpl)WebUtils.getServletContextManager().getActor(nds.util.WebKeys.WEB_CONTROLLER);
			DefaultWebEvent event=new DefaultWebEvent("CommandEvent");


			event.setParameter("command", "nds.weixinpublicparty.ext.VIPBirthdaySendCouponCommand");
			vh= controller.handleEvent(event);
		}catch(Exception e){
			log.debug("call nds.weixinpublicparty.ext.VIPBirthdaySendCouponCommand error->"+e.getLocalizedMessage());
			e.printStackTrace();
		}
		return null;
	}

}
