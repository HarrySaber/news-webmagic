package pageprocessor;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.ConsolePipeline;
import us.codecraft.webmagic.pipeline.FilePipeline;
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.List;

/**
 * PageProcessorDemo
 *
 * @author D.jin
 * @date 2018/5/22
 */
public class PageProcessorDemo implements PageProcessor {
    private Site site = Site.me().setSleepTime(1000).setRetryTimes(3);

    public static final String URL_LIST = "http://www\\.36dm\\.com/sort-4-\\d+\\.html";
    public static final String URL_POST = "http://www\\.36dm\\.com/show-\\w+\\.html";

    @Override
    public Site getSite() {
        return site;
    }

    @Override
    public void process(Page page) {
        if (page.getUrl().regex(URL_LIST).match()) {
            List<String> l_post = page.getHtml().xpath("//div[@class=\"clear\"]").links().regex(URL_POST).all();
            List<String> l_url = page.getHtml().links().regex(URL_LIST).all();
            page.addTargetRequests(l_post);
            page.addTargetRequests(l_url);
        } else {
            String title = page.getHtml().xpath("//div[@class='location']").regex("\\[[\\S|\\s]+\\<").toString();
            page.putField("title", title.substring(0, title.length() - 1).trim());
            page.putField("torrent", page.getHtml().xpath("//p[@class='original download']").links().toString().trim());
            System.out.println();
        }
    }

    public static void main(String[] args) {
        Spider.create(new PageProcessorDemo())
                .addUrl("http://www.36dm.com/sort-4-1.html")
                .addPipeline(new ConsolePipeline())
                .addPipeline(new FilePipeline("E:\\webmagic\\AniMusic"))
                .thread(5)
                .run();
    }
}
