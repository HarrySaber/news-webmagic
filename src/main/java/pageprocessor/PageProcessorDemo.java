package pageprocessor;

import com.google.inject.Inject;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import mapper.UserMapper;
import model.User;
import module.MybatisFactory;
import org.apache.ibatis.session.SqlSession;
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

    private static final Config config = ConfigFactory.load();

    private Site site = Site.me().setSleepTime(1000).setRetryTimes(3);

    public static final String URL_LIST = "http://www\\.36dm\\.com/sort-4-\\d+\\.html";
    public static final String URL_POST = "http://www\\.36dm\\.com/show-\\w+\\.html";

  /*  @Inject
    static UserMapper userMapper;*/

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
        SqlSession sqlSession = MybatisFactory.getFactory().init(config);
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        User user = userMapper.getUserById("1");
        System.out.println(user);
        //userMapper.getUserById("1");
    }
}
