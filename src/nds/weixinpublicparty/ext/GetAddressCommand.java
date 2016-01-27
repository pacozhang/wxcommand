package nds.weixinpublicparty.ext;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nds.control.ejb.Command;
import nds.control.event.DefaultWebEvent;
import nds.control.util.ValueHolder;
import nds.query.QueryEngine;
import nds.util.Configurations;
import nds.util.NDSException;

public class GetAddressCommand extends Command {

	@Override
	public ValueHolder execute(DefaultWebEvent event) throws NDSException,
			RemoteException {
		//获取用户
		ValueHolder vh =new ValueHolder();
		
		JSONArray provinceja=null;
		JSONObject cityjo=new JSONObject();
		JSONObject areajo=new JSONObject();
		String mkey=null;
		try {
			Connection con=QueryEngine.getInstance().getConnection();
			
			provinceja=QueryEngine.getInstance().doQueryObjectArray("select p.id \"id\",p.name \"name\" from c_province p", new Object[] {},con);
			JSONArray cityja=QueryEngine.getInstance().doQueryObjectArray("select c.id \"id\",c.name \"name\",c.c_province_id \"provinceid\" from c_city c", new Object[] {},con);
			JSONArray areaja=QueryEngine.getInstance().doQueryObjectArray("select d.id \"id\",d.name \"name\",d.c_city_id \"cityid\" from c_area d", new Object[] {},con);
			
			if(cityja!=null&&cityja.length()>0) {
				JSONObject mcityjo=null;
				for(int i=0;i<cityja.length();i++) {
					mcityjo=cityja.optJSONObject(i);
					if(mcityjo!=null&&mcityjo!=JSONObject.NULL&&mcityjo.has("PROVINCEID")) {
						mkey="PROVINCE"+mcityjo.optInt("PROVINCEID");
						try {
							if(!cityjo.has(mkey)) {
								cityjo.put(mkey, new JSONArray());
							}
							cityjo.optJSONArray(mkey).put( mcityjo);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
			}
			if(areaja!=null&&areaja.length()>0) {
				JSONObject mareajo=null;
				for(int i=0;i<areaja.length();i++) {
					mareajo=areaja.optJSONObject(i);
					if(mareajo!=null&&mareajo!=JSONObject.NULL&&mareajo.has("CITYID")) {
						mkey="CITY"+mareajo.optInt("CITYID");
						try {
							if(!areajo.has(mkey)) {
								areajo.put(mkey, new JSONArray());
							}
							areajo.optJSONArray(mkey).put(mareajo);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
			}
			con.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		StringBuffer addresssb=new StringBuffer();
		addresssb.append("var province=");
		addresssb.append(provinceja.toString());
		
		addresssb.append(System.getProperty("line.separator"));
		addresssb.append("var city=");
		addresssb.append(cityjo.toString());
		
		addresssb.append(System.getProperty("line.separator"));
		addresssb.append("var area=");
		addresssb.append(areajo.toString());
		
		//写入文件
		Configurations conf=(Configurations)nds.control.web.WebUtils.getServletContextManager().getActor(nds.util.WebKeys.CONFIGURATIONS);	    
		String clientWebRoot=conf.getProperty("dir.nea.root","");
		
		if(nds.util.Validator.isNull(clientWebRoot)) {
			logger.debug("system start path is null->");
			vh.put("code","-1");
		    vh.put("message","操作失败");
		    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
		    vh.put("data",df.format(new Date()));
		    return vh;
		}
		clientWebRoot=clientWebRoot.replace("/act.nea", "");
		
		String filename=null;
		filename=clientWebRoot+"/portal422/server/default/deploy/nds.war/html/nds/oto/shop/app/usercenter/pages/comment/js/address.js";
		//filename=clientWebRoot+"/portal422/server/default/deploy/nds.war/html/nds/oto/webapp/userinfo/js/address.js";
		File file = new File(filename);
		// if file doesnt exists, then create it
		
		String fn=clientWebRoot+"/portal422/server/default/deploy/nds.war/html/nds/oto/shop/app/webmall/pages/comment/js/address.js";
		
		File f=new File(fn);
		
		if(!f.exists()){
			try{
				file.createNewFile();
			}catch(Exception e){
				logger.error("create fine error:"+e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
		try{
			BufferedOutputStream Buff = new BufferedOutputStream(new FileOutputStream(fn));   
			Buff.write(addresssb.toString().getBytes("UTF-8"));
			Buff.flush();   
			Buff.close();
		}catch(Exception e){
			logger.error("write error:"+e.getLocalizedMessage());
			e.printStackTrace();
		}
		
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				logger.debug("create file["+filename+"] error->"+e.getLocalizedMessage());
				e.printStackTrace();
				vh.put("code","-1");
			    vh.put("message","操作失败");
			    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
			    vh.put("data",df.format(new Date()));
			    return vh;
			}
		}
		
		try {
			BufferedOutputStream Buff = new BufferedOutputStream(new FileOutputStream(filename));   
			Buff.write(addresssb.toString().getBytes("UTF-8"));
			Buff.flush();   
			Buff.close();
			
			vh.put("code","0");
		    vh.put("message","操作成功");
		} catch (IOException e) {
			vh.put("code","-1");
		    vh.put("message","操作失败");
		    logger.debug("write address.js error->"+e.getLocalizedMessage());
			e.printStackTrace();
			return vh;
		}
		
		 vh.put("code","0");
	     vh.put("message","操作成功");
	     
	     return vh;
	}

}
