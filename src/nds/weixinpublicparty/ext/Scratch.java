package nds.weixinpublicparty.ext;

import java.util.Hashtable;

import nds.log.Logger;
import nds.log.LoggerManager;
import nds.query.QueryEngine;
import nds.query.QueryException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Scratch {
	private static Hashtable<String, Scratch> factorys;
	private String adClientId;

	private Scratch() {
	};

	public static synchronized Scratch getInstance(String adClientId) {
		if (adClientId == null)
			return null;

		Scratch instance = null;
		if (factorys == null) {
			factorys = new Hashtable<String, Scratch>();
			instance = new Scratch();
			factorys.put(adClientId, instance);
		} else if (factorys.containsKey(adClientId)) {
			instance = factorys.get(adClientId);
		} else {
			instance = new Scratch();
			factorys.put(adClientId, instance);
		}
		instance.adClientId = adClientId;
		return instance;
	}

	/**
	 * 
	 * @param adClientId
	 *            公司ID
	 * @param wxScratchticketId
	 *            活动ID
	 * @param wxVipId
	 *            会员ID
	 * @return code 是否中奖（0为中奖，其他为失败） prizetype几等奖 wx_scratchticket_note_id中奖纪录id
	 *         name奖项名称 rewardtype奖项类型
	 * @throws NumberFormatException
	 * @throws JSONException
	 * @throws QueryException
	 */
	public synchronized String scratch(String adClientId, String wxScratchticketId,
			String wxVipId) throws NumberFormatException, JSONException,
			QueryException {
		

 
		// 查询vip是否中奖
		String recordCountStr = "select count(*) from WX_SCRATCHTICKET_NOTE t where t.ad_client_id = ? and t.wx_scratchticket_id = ? and t.wx_vip_id = ? and t.wx_scratchreward_id is not null";
		int recordCount = Integer.parseInt(QueryEngine
				.getInstance()
				.doQueryOne(recordCountStr.toString(),
						new Object[] { adClientId, wxScratchticketId, wxVipId })
				.toString());
		if (recordCount >= 1) {
			return "{\"code\":\"-1\",\"message\":\"您本次活动已经抽中奖品，则不可在刮了！\"}";
		}
		
		// 查询每人最多允许抽奖次数和中奖率
		String queryMAXTIMES = "select t.MAXTIMES,t.REWARDRATE from WX_SCRATCHTICKET t where t.ad_client_id = ? and t.id = ? ";
		JSONObject bigwheelJson = QueryEngine.getInstance().doQueryObject(
				queryMAXTIMES.toString(),
				new Object[] { adClientId, wxScratchticketId });

		int maxtimes = bigwheelJson.optInt("MAXTIMES");
		double rewardrate = bigwheelJson.optDouble("REWARDRATE");

		// 查询vip已经抽奖次数
		String queryUse = "select count(*) from WX_SCRATCHTICKET_NOTE t where t.ad_client_id = ? and t.wx_scratchticket_id = ? and t.wx_vip_id = ? ";
		int useCount = Integer.parseInt(QueryEngine
				.getInstance()
				.doQueryOne(queryUse.toString(),
						new Object[] { adClientId, wxScratchticketId, wxVipId })
				.toString());
		int remainCount = maxtimes - useCount;
		if (remainCount <= 0) {
			return "{\"code\":\"-1\",\"message\":\"抽奖次数已经用完\"}";
		}

		/*
		 * 查询奖项设置信息 rewarddegree:奖项等级 wx_scratchreward_id id： 奖品ID
		 * rewardtype：奖品类型 name：奖品名称 wx_coupon_id：优惠券ID integral：积分数
		 */
		/*
		StringBuffer queryAward = new StringBuffer();

		queryAward
				.append("select t.rewarddegree,t.wx_scratchreward_id,b.id,b.rewardtype,b.name,b.wx_coupon_id,b.integral from wx_scratchticketitem t, WX_SCRATCHREWARD b ");
		queryAward
				.append(" where  t.ad_client_id = ? and t.wx_scratchticket_id = ? and ");
		queryAward
				.append(" (select count(1) from wx_scratchticket_note a where a.ad_client_id = ? and a.wx_scratchticket_id = ? and a.wx_scratchreward_id = t.wx_scratchreward_id) < t.rewardcount and t.wx_scratchreward_id = b.id");
		JSONArray array = QueryEngine.getInstance().doQueryObjectArray(
				queryAward.toString(),
				new Object[] { adClientId, wxScratchticketId, adClientId,
						wxScratchticketId });
		*/
		
		int recordId = QueryEngine.getInstance().getSequence(
				"WX_SCRATCHTICKET_NOTE");

		StringBuffer insertSQL = new StringBuffer();

		insertSQL
				.append("insert into WX_SCRATCHTICKET_NOTE(id,ad_client_id,ad_org_id,wx_scratchticket_id,wx_scratchreward_id,wx_vip_id,record_state,receivetime,ownerid,modifierid,creationdate,modifieddate,isactive) ");
		insertSQL
				.append("select ?,t.id,t.ad_org_id,?,?,?,1,sysdate,t.ownerid,t.modifierid,sysdate,sysdate,'Y' from ad_client t where t.id = ?");

		int temp = (int) Math.round(Math.random() * 10000);// 随见产生一个数
		int max = (int) Math.round(rewardrate / 100.0 * 10000);// 通过概率算区间
		
		String sj = "SELECT i.wx_scratchreward_id, i.rewardcount, s.name, i.rewarddegree,s.rewardtype, s.wx_coupon_id, s.integral,nvl(sc.\"wincount\",0) \"wincount\",i.rewardcount-nvl(sc.\"wincount\",0) \"surpluscount\" FROM wx_scratchticketitem i LEFT JOIN (SELECT s.wx_scratchticket_id, s.wx_scratchreward_id,COUNT(1) \"wincount\" FROM wx_scratchticket_note s WHERE s.wx_scratchreward_id IS NOT NULL AND s.wx_scratchticket_id = ? GROUP BY s.wx_scratchticket_id, s.wx_scratchreward_id) sc ON i.wx_scratchreward_id = sc.wx_scratchreward_id LEFT JOIN wx_scratchreward s ON s.id = i.wx_scratchreward_id WHERE i.wx_scratchticket_id = ? and nvl(i.rewardcount,0)>nvl(sc.\"wincount\",0) order by i.rewarddegree asc";
		JSONArray jarray = QueryEngine.getInstance().doQueryObjectArray(sj, new Object[]{wxScratchticketId,wxScratchticketId});

		if (jarray == null || jarray.length() <= 0 || temp > max) {
			// 插入中奖纪录
			QueryEngine.getInstance().executeUpdate(
					insertSQL.toString(),
					new Object[] { recordId, wxScratchticketId, "", wxVipId,
							adClientId });
			return "{\"code\":\"-1\",\"message\":\"未抽中奖品\"}";
		}

		// 如果temp<=max则为中奖
		
		String Prize = "select sum(nvl(s.rewardcount,0)),sum(nvl(sn.\"count\", 0)) from wx_scratchticketitem s left join (select count(1) \"count\",cn.wx_scratchreward_id from wx_scratchticket_note cn where cn.wx_scratchticket_id=? and cn.wx_scratchreward_id is not null group by cn.wx_scratchreward_id) sn on s.wx_scratchreward_id=sn.wx_scratchreward_id where s.wx_scratchticket_id=?";
		JSONArray ja = QueryEngine.getInstance().doQueryJSONArray(Prize,new Object[]{wxScratchticketId,wxScratchticketId});
		
		long sumcount=ja.optJSONArray(0).optLong(0);
		long wincount=ja.optJSONArray(0).optLong(1);
		long surpluscount=sumcount-wincount;
		
		
		long start=0;
		int index = 0;
		long boundary=0;
		long sboundary=0;
		int startindex=0;
		long randowvalue=-1;
		int length=jarray.length();
		System.out.println("nds.weixin.ext.Scratch length->"+length+",sumprize->"+sumcount+",winprize->"+wincount+",surplusprize->"+surpluscount);
		
		if(length>1) {
			if((sumcount / 2) > wincount) {
				startindex=1;
				start=jarray.optJSONObject(0).optLong("SURPLUSCOUNT");
			}
			System.out.println("nds.weixin.ext.Scratch startindex->"+startindex+",start->"+start);
			randowvalue=(long)(Math.random() * (surpluscount-start));
			System.out.println("nds.weixin.ext.Scratch randowvalue->"+randowvalue);
			
			for(int i=startindex;i<length;i++) {
				boundary=jarray.optJSONObject(i).optLong("SURPLUSCOUNT");
				System.out.println("nds.weixin.ext.Scratch sboundary->"+sboundary+",boundary->"+boundary);
				if(randowvalue>=sboundary&&randowvalue<(sboundary+boundary)) {
					index=i;
					break;
				}
				sboundary+=boundary;
			}
		}
		System.out.println("nds.weixin.ext.Scratch index->"+index);
		
		
		// 如果中奖了,从奖品中任意取一件
		// int index = (int) Math.random() * array.length();
		QueryEngine.getInstance().executeUpdate(
				insertSQL.toString(),
				new Object[] {
						recordId,
						wxScratchticketId,
						jarray.getJSONObject(index).getString("WX_SCRATCHREWARD_ID")
						, wxVipId, adClientId });

		return "{\"code\":\"0\",\"prizetype\":"
				+ jarray.getJSONObject(index).getString("REWARDDEGREE")
				+ ",\"wx_scratchticket_note_id\":" + recordId + ",\"name\":\""
				+ jarray.getJSONObject(index).getString("NAME")
				+ "\",\"rewardtype\":\""
				+ jarray.getJSONObject(index).getString("REWARDTYPE")
				+ "\",\"message\":\"恭喜你中奖了\"}";
	}
}
