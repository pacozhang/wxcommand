package nds.weixinpublicparty.ext;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.jfree.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import nds.control.ejb.Command;
import nds.control.event.DefaultWebEvent;
import nds.control.util.ValueHolder;
import nds.publicweixin.ext.tools.RestUtils;
import nds.query.QueryEngine;
import nds.query.QueryException;
import nds.util.NDSException;
import nds.util.Tools;

public class VIPBirthdaySendCouponCommand extends Command {

	@Override
	public ValueHolder execute(DefaultWebEvent event) throws NDSException,
			RemoteException {
		ValueHolder vh=new ValueHolder();
		
		Hashtable<Integer,List> compaoninf=new Hashtable<Integer,List>();
		
		String sql="select bc.ad_client_id,bc.id,cp.num,nvl(cp.usetype1,2),nvl(cp.value,'0'),to_char(nvl(cp.starttime,sysdate), 'YYYYMMDD'),to_char(decode(nvl(cp.validay,0),0, nvl(cp.endtime, add_months(sysdate,1)),nvl(cp.starttime,sysdate)+nvl(cp.validay,0)), 'YYYYMMDD')"
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
		Connection con=null;
		
		try {
			QueryEngine qe=null;
			
			try {
				qe = QueryEngine.getInstance();
				con=qe.getConnection();
			} catch (Exception e) {
				e.printStackTrace();
				vh.put("code", "-1");
				vh.put("message", "失败");
				return vh;
			}
			
			List allcoupons=null;
			try {
				allcoupons = qe.doQueryList(sql,con);
			} catch (QueryException e) {
				Log.debug("search wx_autosendcoupon error->"+e.getLocalizedMessage());
				e.printStackTrace();
				try {
					if(con!=null) {
						con.close();
					}
				}catch(Exception e1) {
					Log.debug("search wx_autosendcoupon cloas connectiion error->"+e1.getLocalizedMessage());
					e1.printStackTrace();
				}
			}
			
			if(allcoupons==null||allcoupons.size()<=0){
				vh.put("code", "-1");
				vh.put("message", "没有符合的数据");
				return vh;
			}
			
			
			List vips=null;
			
			List all=null;
			List cone=null;
			int ad_client_id=0;
			List couponone=null;
			int length=allcoupons.size();
			
			String coupontypecode=null;
			String coupontype=null;
			String couponvalue=null;
			String begintime=null;
			String endtime=null;
			boolean iserp=false;
			String openid=null;
			int vipcount=0;
			String vipcardno=null;
			List vipone=null;
			String skey=null;
			String erpurl=null;
			String result=null;
			JSONObject jo=null;
			String ticketno=null;
			JSONObject consumejo =null;
			int vipid=0;
			int bcid=0;
			//发券记录数
			int countlog=0;
			
			HashMap<String, String> params=null;
			
			for(int i=0;i<length;i++) {
				couponone=(List)allcoupons.get(i);
				ad_client_id=Tools.getInt(couponone.get(0), -1);
				bcid=Tools.getInt(couponone.get(1), -1);
				
				if(ad_client_id<=0) {
					Log.debug("ad_client_id error->"+ad_client_id);
					continue;
				}
				try {
					if(compaoninf.containsKey(ad_client_id)) {
						cone=compaoninf.get(ad_client_id);
					}else {
						try {
							//查询接口相关信息 url,skey
							all=QueryEngine.getInstance().doQueryList("select ifs.erpurl,wc.wxparam,ifs.iserp from WX_INTERFACESET ifs join web_client wc on ifs.ad_client_id=wc.ad_client_id WHERE ifs.ad_client_id="+ad_client_id);
							if(all==null||all.size()<=0) {
								continue;
							}
							
							cone=(List)all.get(0);
							if(cone==null||cone.size()<3) {
								continue;
							}
							compaoninf.put(ad_client_id, cone);
						}catch(Exception e) {
							Log.debug("search company error->"+e.getLocalizedMessage());
							e.printStackTrace();
							continue;
						}
					}
					vips=qe.doQueryList(vipsql,new Object[] {ad_client_id},con);
				} catch (QueryException e) {
					e.printStackTrace();
					try {
						if(con!=null) {
							con.close();
						}
					}catch(Exception e1) {
						Log.debug("search vip cloas connectiion error->"+e1.getLocalizedMessage());
						e1.printStackTrace();
					}
					continue;
				}
				
				if(vips==null||vips.size()<=0) {continue;}
				
				vipcount=vips.size();
				couponvalue=String.valueOf(couponone.get(4));
				coupontype=String.valueOf(couponone.get(3));
				coupontypecode=String.valueOf(couponone.get(2));
				begintime=String.valueOf(couponone.get(5));
				endtime=String.valueOf(couponone.get(6));
				
				skey=String.valueOf(cone.get(1));
				erpurl=String.valueOf(cone.get(0));
				iserp="Y".equalsIgnoreCase(String.valueOf(cone.get(2)));
				for(int j=0;j<vipcount;j++) {
					vipone=(List)vips.get(j);
					ticketno="";
					
					//线下发券
					if(iserp&&!"1".equals(coupontype)) {
						openid=String.valueOf(vipone.get(1));
						vipcardno=String.valueOf(vipone.get(2));
						params =new HashMap<String, String>();
						String ts=String.valueOf(System.currentTimeMillis());
						Log.debug("ts->"+ts);
						params.put("args[cardid]",String.valueOf(ad_client_id));
						params.put("args[openid]",openid);
						params.put("args[vipno]",vipcardno);
						params.put("args[couponno]",coupontypecode);
						params.put("args[couponvalue]",couponvalue);				
						params.put("args[begintime]",begintime);
						params.put("args[endtime]",endtime);
						
						params.put("format","JSON");
						params.put("client","");
						params.put("ver","1.0");
						params.put("ts",ts);
						try {
							params.put("sig",nds.util.MD5Sum.toCheckSumStr(String.valueOf(ad_client_id) + ts+ skey));
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						params.put("method","sendcoupon");
						
						Log.debug("params->"+params);
						try{
							Log.debug("welcome!!!");
							vh=RestUtils.sendRequest(erpurl,params,"POST");
							Log.debug("vh->"+vh);
					    } catch (Throwable e) {
							   Log.debug("ERP网络通信障碍!"+e.getLocalizedMessage());
						}
						result=(String) vh.get("message");
						logger.debug("birthday coupon offline code result->"+result);
						try {
							jo= new JSONObject(result);
							if(jo.optInt("errCode",-1)==0) {
								vh.put("code", "0");
								vh.put("message", "发劵成功");
								vh.put("data", "发劵成功");
								
								ticketno = jo.optJSONObject("result").optJSONObject("data").optString("code");
							}else{
								vh.put("code", "-1");
								vh.put("message", "发劵失败");
								continue;
							}
						}catch(Exception e) {
							
						}
					}
					
					
					//线上发券
					vipid=Tools.getInt(vipone.get(0), -1);
					try {
						consumejo=new JSONObject();
						consumejo.put("vipid", vipid);
						consumejo.put("couponcode",coupontypecode);
						consumejo.put("tickno", ticketno);
						
						String resultStr=null;
						ArrayList oparams=new ArrayList();
						oparams.add(ad_client_id);
						oparams.add(consumejo.toString());
						logger.debug("vipbirthday send coupon call oracle params->"+consumejo.toString());
						ArrayList returnpara=new ArrayList();
						returnpara.add( java.sql.Clob.class);
						Collection list=QueryEngine.getInstance().executeFunction("wx_coupon_$r_send", oparams, returnpara);
						resultStr=(String)list.iterator().next();
						logger.debug("打印结果："+resultStr);
						// 这个成功一次，记录加一
						countlog=countlog+1;
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				//更新是否发放
				int updatebirthdaycoupons=0;
				try {
					updatebirthdaycoupons=qe.executeUpdate(updatesql, new Object[] {bcid},con);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				
			}
			//这个判断记录数，大于0就成功
			if(countlog>0){
				vh.put("code", "0");
				vh.put("message", "发券成功");
			}else{
				vh.put("code", "-1");
				vh.put("message", "发券失败");
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}finally{
			if(con!=null){
				try{
					con.close();
				}catch(Exception e){
					
				}
			}
		}
		
		return vh;
	}

}
