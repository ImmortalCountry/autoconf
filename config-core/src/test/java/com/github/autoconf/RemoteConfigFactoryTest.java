package com.github.autoconf;

import com.github.autoconf.api.IChangeListener;
import com.github.autoconf.api.IChangeableConfig;
import com.github.autoconf.api.IConfig;
import com.github.autoconf.base.ProcessInfo;
import com.google.common.io.Closeables;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.ZKPaths;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * 测试工厂类
 * Created by lirui on 2015-10-01 23:01.
 */
public class RemoteConfigFactoryTest {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteConfigFactoryTest.class);
  private static TestingServer server;

  @BeforeClass
  public static void beforeClass() throws Exception {
    server = new TestingServer();
    //设置环境变量,覆盖application.properties配置
    System.setProperty("zookeeper.servers", server.getConnectString());
  }

  @AfterClass
  public static void afterClass() throws Exception {
    Closeables.close(server, true);
  }

  @Test
  public void testFactory() throws Exception {
    RemoteConfigFactory factory = RemoteConfigFactory.getInstance();
    ProcessInfo info = factory.getInfo();
    String name = "app.ini";
    String path = ZKPaths.makePath(info.getPath(), name, info.getProfile());
    final AtomicInteger num = new AtomicInteger(0);
    IChangeableConfig c = factory.getConfig(name, new IChangeListener() {
      @Override
      public void changed(IConfig config) {
        num.incrementAndGet();
      }
    });
    create(factory.getClient(), path, newBytes("a=1"));
    busyWait(num);
    assertThat(c.getInt("a"), is(1));

    num.set(0);
    setData(factory.getClient(), path, newBytes("a=2"));
    busyWait(num);
    assertThat(c.getInt("a"), is(2));
  }

  private void busyWait(final AtomicInteger num) throws InterruptedException {
    int tries = 0;
    while (++tries < 600) {
      Thread.sleep(100);
      if (num.get() > 0) {
        LOG.info("delay {} ms", 100 * tries);
        return;
      }
    }
    LOG.error("detect timeout, delay {}ms", 100 * tries);
  }
}