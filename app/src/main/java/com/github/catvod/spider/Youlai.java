package com.github.catvod.spider;

import android.content.Context;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.SSLCompat;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

/*
  @author Qile
 */
public class FirstAid extends Spider {

    private String siteUrl = "https://m.youlai.cn";
    private final String userAgent = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36 Edg/109.0.1518.100";

    @Override
    public void init(Context context, String extend) throws Exception {
        super.init(context, extend);
    }

    @Override
    public String homeContent(boolean filter) {
        try {
            JSONObject jjjn = new JSONObject()
                    .put("type_id", "jijiu|0")
                    .put("type_name", "急救技能");

            JSONObject jtsh = new JSONObject()
                    .put("type_id", "jijiu|1")
                    .put("type_name", "家庭生活");

            JSONObject jwzz = new JSONObject()
                    .put("type_id", "jijiu|2")
                    .put("type_name", "急危重症");

            JSONObject cjss = new JSONObject()
                    .put("type_id", "jijiu|3")
                    .put("type_name", "常见损伤");

            JSONObject dwzs = new JSONObject()
                    .put("type_id", "jijiu|4")
                    .put("type_name", "动物致伤");

            JSONObject hyjj = new JSONObject()
                    .put("type_id", "jijiu|5")
                    .put("type_name", "海洋急救");

            JSONObject zdjj = new JSONObject()
                    .put("type_id", "jijiu|6")
                    .put("type_name", "中毒急救");

            JSONObject ywsg = new JSONObject()
                    .put("type_id", "jijiu|7")
                    .put("type_name", "意外事故");

            JSONArray classes = new JSONArray()
                    .put(jjjn)
                    .put(jtsh)
                    .put(jwzz)
                    .put(cjss)
                    .put(dwzs)
                    .put(hyjj)
                    .put(zdjj)
                    .put(ywsg);

            JSONObject result = new JSONObject()
                    .put("class", classes);

            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        try {
            // 筛选处理
            HashMap<String, String> ext = new HashMap<>();
            if (extend != null && extend.size() > 0) {
                ext.putAll(extend);
            }
            String[] item = tid.split("\\|");
            String id = item[0];
            String digit = item[1];
            int digitValue = Integer.parseInt(digit);
            String cateId = ext.get("cateId") == null ? id : ext.get("cateId");

            // 电影第二页
            String cateUrl = siteUrl + String.format("/%s", cateId);
            String content = getContent(cateUrl);
            Document doc = Jsoup.parse(content);
            String pic = "https:" + doc.select(".block100").eq(digitValue).attr("src");
            Elements lis = Jsoup.parse(content).select(".jj-title-li").eq(digitValue).select(".list-br3");
            JSONArray videos = new JSONArray();
            for (Element li : lis) {
                String vid = siteUrl + li.select("a").attr("href");
                String name = li.select("a").text();

                JSONObject vod = new JSONObject()
                        .put("vod_id", vid)
                        .put("vod_name", name)
                        .put("vod_pic", pic);
                videos.put(vod);
            }
            JSONObject result = new JSONObject()
                    .put("page", Integer.parseInt(pg))
                    .put("pagecount", 1)
                    .put("limit", lis.size())
                    .put("total", lis.size())
                    .put("list", videos);
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public String detailContent(List<String> ids) {
        try {
            String detailUrl = ids.get(0);
            String content = getContent(detailUrl);
            Document doc = Jsoup.parse(content);

            String title = doc.select(".video-title.h1-title").text();
            String pic = doc.select(".video-cover.list-flex-in img").attr("src"); // 图片
            String actor = doc.select("span.doc-name").text(); // 演员
            String area = "中国"; // 地区
            String brief = doc.select(".img-text-con").text(); // 简介
            String vod_play_from = "Qile";
            String play_url = doc.select("#video source").attr("src");
            String vod_play_url = title + "$" + play_url;

            JSONObject info = new JSONObject()
                    .put("vod_id", ids.get(0)) // 必填
                    .put("vod_name", title)
                    .put("vod_pic", pic)
                    .put("vod_area", area) // 选填
                    .put("vod_actor", actor) // 选填
                    .put("vod_content", brief) // 选填
                    .put("vod_play_from", vod_play_from) // 必须有，否则播放可能存在问题
                    .put("vod_play_url", vod_play_url.toString()); // 必须有，否则播放可能存在问题

            JSONArray list_info = new JSONArray()
                    .put(info);
            JSONObject result = new JSONObject()
                    .put("list", list_info);
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public String searchContent(String key, boolean quick) {
        try {
            // https://m.youlai.cn/cse/search?q=%E4%BA%BA%E5%B7%A5%E5%91%BC%E5%90%B8
            String url = siteUrl + "/cse/search?q=" + URLEncoder.encode(key);
            String content = getContent(url);
            Elements lis = Jsoup.parse(content)
                    .select(".search-video-li.list-br2");
            JSONArray videos = new JSONArray();
            for (Element li : lis) {
                String vid = siteUrl + li.select("a").attr("href");
                String name = li.select("h5.line-clamp1").text();
                String pic = li.select("dt.logo-bg img").attr("src");
                if (!pic.startsWith("https")) {
                    pic = "https:" + pic;
                }
                JSONObject vod = new JSONObject()
                        .put("vod_id", vid)
                        .put("vod_name", name)
                        .put("vod_pic", pic);
                videos.put(vod);
            }
            JSONObject result = new JSONObject()
                    .put("list", videos);
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        try {
            JSONObject result = new JSONObject()
                    .put("parse", 0)
                    .put("url", id);
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getContent(String targetUrl) throws IOException {
        Request request = new Request.Builder()
                .url(targetUrl)
                .get()
                .addHeader("User-Agent", userAgent)
                .build();
        OkHttpClient okHttpClient = new OkHttpClient()
                .newBuilder()
                .sslSocketFactory(new SSLCompat(), SSLCompat.TM) // 取消证书认证
                .build();
        Response response = okHttpClient.newCall(request).execute();
        String content = response.body().string();
        response.close();
        return content;
    }

}
