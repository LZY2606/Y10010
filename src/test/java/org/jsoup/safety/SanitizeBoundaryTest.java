package org.jsoup.safety;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 Boundary tests pinning the observable differences between the public sanitize entry points
 (Jsoup.clean / Jsoup.isValid / Cleaner.clean / Cleaner.isValid / Cleaner.isValidBodyHtml) for
 identical input and safelist. Expectations were captured from the current implementation; any
 behavioral change in Cleaner, Safelist, or Jsoup should turn these red deliberately.
 */
public class SanitizeBoundaryTest {
    private static final String AnchorHtml = "<a href='/foo'>Link</a>";
    private static final String ImgHtml = "<img src='/i.png'>";

    // mutant: Jsoup#clean 把 相对链接丢弃 改成 相对链接保留
    @Test public void entryMatrixWithDefaultPreserveRelativeLinksFalse() {
        for (Safelist safelist : new Safelist[]{Safelist.basic(), Safelist.basicWithImages()}) {
            assertEquals("<a rel=\"nofollow\">Link</a>", Jsoup.clean(AnchorHtml, safelist));
            assertEquals("<a rel=\"nofollow\">Link</a>", Jsoup.clean(AnchorHtml, "", safelist));
            assertEquals("<a rel=\"nofollow\">Link</a>", Jsoup.clean(AnchorHtml, "", safelist, new Document.OutputSettings()));
            assertEquals("<a rel=\"nofollow\">Link</a>",
                new Cleaner(safelist).clean(Jsoup.parseBodyFragment(AnchorHtml, "")).body().html());
            assertFalse(Jsoup.isValid(AnchorHtml, safelist));
            assertFalse(new Cleaner(safelist).isValid(Jsoup.parseBodyFragment(AnchorHtml, "")));
            assertFalse(new Cleaner(safelist).isValidBodyHtml(AnchorHtml));
        }
    }

    // mutant: Safelist#isSafeTag 把 img 判定为不安全 改成 判定为安全
    @Test public void entryMatrixImgDroppedByBasicKeptByBasicWithImages() {
        Safelist basic = Safelist.basic();
        assertEquals("", Jsoup.clean(ImgHtml, basic));
        assertEquals("", Jsoup.clean(ImgHtml, "", basic));
        assertEquals("", Jsoup.clean(ImgHtml, "", basic, new Document.OutputSettings()));
        assertEquals("", new Cleaner(basic).clean(Jsoup.parseBodyFragment(ImgHtml, "")).body().html());
        assertFalse(Jsoup.isValid(ImgHtml, basic));
        assertFalse(new Cleaner(basic).isValid(Jsoup.parseBodyFragment(ImgHtml, "")));
        assertFalse(new Cleaner(basic).isValidBodyHtml(ImgHtml));

        Safelist withImages = Safelist.basicWithImages();
        assertEquals("<img>", Jsoup.clean(ImgHtml, withImages));
        assertEquals("<img>", Jsoup.clean(ImgHtml, "", withImages));
        assertEquals("<img>", Jsoup.clean(ImgHtml, "", withImages, new Document.OutputSettings()));
        assertEquals("<img>", new Cleaner(withImages).clean(Jsoup.parseBodyFragment(ImgHtml, "")).body().html());
        assertFalse(Jsoup.isValid(ImgHtml, withImages));
        assertFalse(new Cleaner(withImages).isValid(Jsoup.parseBodyFragment(ImgHtml, "")));
        assertFalse(new Cleaner(withImages).isValidBodyHtml(ImgHtml));
    }

    // mutant: Jsoup#clean 把 空 baseUri 替换为 DummyUri 改成 保持空 baseUri
    @Test public void preserveRelativeLinksJsoupCleanKeepsRelativeUrls() {
        Safelist basic = Safelist.basic().preserveRelativeLinks(true);
        // rel=nofollow is also dropped: the dummy base host matches the resolved link host
        assertEquals("<a href=\"/foo\">Link</a>", Jsoup.clean(AnchorHtml, basic));
        assertEquals("<a href=\"/foo\">Link</a>", Jsoup.clean(AnchorHtml, "", basic));
        assertTrue(Jsoup.isValid(AnchorHtml, basic));
        assertTrue(new Cleaner(basic).isValidBodyHtml(AnchorHtml));

        Safelist withImages = Safelist.basicWithImages().preserveRelativeLinks(true);
        assertEquals("<img src=\"/i.png\">", Jsoup.clean(ImgHtml, withImages));
        assertEquals("<img src=\"/i.png\">", Jsoup.clean(ImgHtml, "", withImages));
        assertTrue(Jsoup.isValid(ImgHtml, withImages));
        assertTrue(new Cleaner(withImages).isValidBodyHtml(ImgHtml));
    }

    // mutant: Jsoup#clean 把 四参重载不做 DummyUri 替换 改成 与三参重载一致替换
    @Test public void cleanWithOutputSettingsDivergesWhenPreservingRelativeLinks() {
        Safelist basic = Safelist.basic().preserveRelativeLinks(true);
        assertEquals("<a rel=\"nofollow\">Link</a>",
            Jsoup.clean(AnchorHtml, "", basic, new Document.OutputSettings()));

        Safelist withImages = Safelist.basicWithImages().preserveRelativeLinks(true);
        assertEquals("<img>", Jsoup.clean(ImgHtml, "", withImages, new Document.OutputSettings()));
    }

    // mutant: Cleaner#clean 把 使用文档自带 baseUri 改成 空 baseUri 时替换为 DummyUri
    @Test public void cleanerCleanOnEmptyBaseUriDocDivergesWhenPreservingRelativeLinks() {
        Safelist basic = Safelist.basic().preserveRelativeLinks(true);
        assertEquals("<a rel=\"nofollow\">Link</a>",
            new Cleaner(basic).clean(Jsoup.parseBodyFragment(AnchorHtml, "")).body().html());

        Safelist withImages = Safelist.basicWithImages().preserveRelativeLinks(true);
        assertEquals("<img>",
            new Cleaner(withImages).clean(Jsoup.parseBodyFragment(ImgHtml, "")).body().html());
    }

    // mutant: Cleaner#isValid 把 使用文档自带 baseUri 改成 使用 DummyUri
    @Test public void cleanerIsValidOnEmptyBaseUriDocDivergesFromJsoupIsValid() {
        Safelist basic = Safelist.basic().preserveRelativeLinks(true);
        assertFalse(new Cleaner(basic).isValid(Jsoup.parseBodyFragment(AnchorHtml, "")));
        assertTrue(Jsoup.isValid(AnchorHtml, basic));

        Safelist withImages = Safelist.basicWithImages().preserveRelativeLinks(true);
        assertFalse(new Cleaner(withImages).isValid(Jsoup.parseBodyFragment(ImgHtml, "")));
        assertTrue(Jsoup.isValid(ImgHtml, withImages));
    }

    // mutant: Safelist#isSafeProtocol 把 要求协议名后紧跟冒号 改成 仅匹配前缀
    @Test public void protocolPrefixWithoutColonIsRejected() {
        Safelist safelist = Safelist.basic();
        assertEquals("<a rel=\"nofollow\">x</a>", Jsoup.clean("<a href='httpx://example.com'>x</a>", "", safelist));
        assertEquals("<a rel=\"nofollow\">x</a>", Jsoup.clean("<a href='httpsfoo'>x</a>", "", safelist));
        assertEquals("<a rel=\"nofollow\">x</a>", Jsoup.clean("<a href='https-x://e.com'>x</a>", "", safelist));
    }

    // mutant: Safelist#isSafeProtocol 把 协议匹配前转小写 改成 大小写敏感匹配
    @Test public void mixedCaseSchemeIsLowercasedButHostCaseIsPreserved() {
        Safelist safelist = Safelist.basic();
        assertEquals("<a href=\"http://Example.COM/A\" rel=\"nofollow\">x</a>",
            Jsoup.clean("<a href='HTTP://Example.COM/A'>x</a>", "", safelist));
    }

    // mutant: Safelist#isValidAnchor 把 拒绝含空白字符 改成 允许任意字符
    @Test public void anchorProtocolRejectsFragmentsWithWhitespace() {
        Safelist safelist = Safelist.basic().addProtocols("a", "href", "#");
        assertEquals("<a rel=\"nofollow\">x</a>", Jsoup.clean("<a href='#has space'>x</a>", "", safelist));
        assertEquals("<a rel=\"nofollow\">x</a>", Jsoup.clean("<a href='#has\ttab'>x</a>", "", safelist));
        assertEquals("<a rel=\"nofollow\">x</a>", Jsoup.clean("<a href='#has\nnl'>x</a>", "", safelist));
    }

    // mutant: Safelist#isValidAnchor 把 要求以 # 开头 改成 允许任意取值
    @Test public void anchorProtocolKeepsPlainFragment() {
        Safelist safelist = Safelist.basic().addProtocols("a", "href", "#");
        assertEquals("<a href=\"#ok\" rel=\"nofollow\">x</a>", Jsoup.clean("<a href='#ok'>x</a>", "", safelist));
    }

    // mutant: Cleaner#createSafeElement 把 强制属性大小写不敏感覆盖 改成 仅精确键匹配覆盖
    @Test public void enforcedRelCanonicalizesKeyAndValueCase() {
        Safelist safelist = Safelist.basic().addEnforcedAttribute("a", "rel", "nofollow");
        String expected = "<a href=\"http://e.com/\" rel=\"nofollow\">x</a>";
        assertEquals(expected, Jsoup.clean("<a href='http://e.com/' rel='nofollow'>x</a>", "", safelist));
        assertEquals(expected, Jsoup.clean("<a href='http://e.com/' rel='NOFOLLOW'>x</a>", "", safelist));
        assertEquals(expected, Jsoup.clean("<a href='http://e.com/' REL='nofollow'>x</a>", "", safelist));
        assertEquals(expected, Jsoup.clean("<a href='http://e.com/' REL='x'>x</a>", "", safelist));
    }

    // mutant: Safelist#isSafeAttribute 把 强制属性值精确比较 改成 忽略大小写比较
    @Test public void enforcedRelValueMatchIsCaseSensitiveForIsValid() {
        Safelist safelist = Safelist.basic().addEnforcedAttribute("a", "rel", "nofollow");
        Cleaner cleaner = new Cleaner(safelist);
        assertTrue(cleaner.isValidBodyHtml("<a href='http://e.com/' rel='nofollow'>x</a>"));
        assertFalse(cleaner.isValidBodyHtml("<a href='http://e.com/' rel='NOFOLLOW'>x</a>"));
        assertTrue(cleaner.isValidBodyHtml("<a href='http://e.com/' REL='nofollow'>x</a>"));
        assertFalse(cleaner.isValidBodyHtml("<a href='http://e.com/' REL='x'>x</a>"));
    }

    // mutant: Cleaner#createSafeElement 把 强制属性移到属性列表末尾 改成 保留源属性位置
    @Test public void enforcedAttributeMovesToEndOfAttributeList() {
        Safelist safelist = Safelist.basic().addEnforcedAttribute("a", "rel", "nofollow");
        assertEquals("<a href=\"http://e.com/\" rel=\"nofollow\">x</a>",
            Jsoup.clean("<a rel='nofollow' href='http://e.com/'>x</a>", "", safelist));
    }

    // mutant: Cleaner#createSafeElement 把 按源顺序保留安全属性 改成 按字母序输出属性
    @Test public void sourceAttributeOrderPreservedWithEnforcedAppendedLast() {
        Safelist safelist = Safelist.basic().addAttributes("a", "title");
        assertEquals("<a title=\"t\" href=\"http://e.com/\" rel=\"nofollow\">x</a>",
            Jsoup.clean("<a title='t' href='http://e.com/'>x</a>", "", safelist));
        assertEquals("<a href=\"http://e.com/\" title=\"t\" rel=\"nofollow\">x</a>",
            Jsoup.clean("<a href='http://e.com/' title='t'>x</a>", "", safelist));
    }

    // mutant: Cleaner#isValid 把 起点节点不计入丢弃数 改成 起点节点也计入
    @Test public void isValidIgnoresBodyRootTagItself() {
        // relaxed() does not allow the body tag, yet the traversal root is not counted as discarded
        Document doc = Jsoup.parse("<html><head></head><body><p>hi</p></body></html>");
        assertTrue(new Cleaner(Safelist.relaxed()).isValid(doc));
    }

    // mutant: Cleaner#isValid 把 检查 head 子节点为空 改成 不检查 head
    @Test public void isValidRejectsDocumentsWithHeadContent() {
        Document doc = Jsoup.parse("<html><head><title>t</title></head><body><p>hi</p></body></html>");
        assertFalse(new Cleaner(Safelist.relaxed()).isValid(doc));
    }

    // mutant: Cleaner#isValid 把 注释计入丢弃数 改成 忽略注释
    @Test public void isValidCountsDiscardedComments() {
        Document doc = Jsoup.parse("<html><head></head><body><!-- c --></body></html>");
        Cleaner cleaner = new Cleaner(Safelist.relaxed());
        assertFalse(cleaner.isValid(doc));
        assertFalse(cleaner.isValidBodyHtml("<!-- c -->"));
    }

    // mutant: Cleaner#isValid 把 文本节点直接拷贝 改成 文本节点计入丢弃数
    @Test public void isValidAcceptsTextOnlyBody() {
        Document doc = Jsoup.parse("<html><head></head><body>hello</body></html>");
        Cleaner cleaner = new Cleaner(Safelist.relaxed());
        assertTrue(cleaner.isValid(doc));
        assertTrue(cleaner.isValidBodyHtml("hello"));
    }

    // mutant: Cleaner#isValidBodyHtml 把 只解析 body 片段 改成 同时校验 head 内容
    @Test public void isValidBodyHtmlMissesHeadContentThatIsValidCatches() {
        Cleaner cleaner = new Cleaner(Safelist.relaxed());
        Document withHead = Jsoup.parse("<html><head><title>t</title></head><body><p>hi</p></body></html>");
        assertFalse(cleaner.isValid(withHead));
        assertTrue(cleaner.isValidBodyHtml("<p>hi</p>"));
    }

    // mutant: Cleaner#clean 把 直接引用传入的 Safelist 改成 构造时深拷贝 Safelist
    @Test public void cleanerObservesLaterSafelistMutation() {
        Safelist safelist = Safelist.basic();
        Cleaner cleaner = new Cleaner(safelist);
        String imgHtml = "<img src='http://e.com/i.png'>";
        assertEquals("", cleaner.clean(Jsoup.parseBodyFragment(imgHtml, "")).body().html());

        safelist.addTags("img").addAttributes("img", "src").addProtocols("img", "src", "http", "https");
        assertEquals("<img src=\"http://e.com/i.png\">",
            cleaner.clean(Jsoup.parseBodyFragment(imgHtml, "")).body().html());

        Safelist protoList = Safelist.basic();
        Cleaner protoCleaner = new Cleaner(protoList);
        String aHtml = "<a href='http://e.com/'>x</a>";
        assertEquals("<a href=\"http://e.com/\" rel=\"nofollow\">x</a>",
            protoCleaner.clean(Jsoup.parseBodyFragment(aHtml, "")).body().html());

        protoList.removeProtocols("a", "href", "http");
        assertEquals("<a rel=\"nofollow\">x</a>",
            protoCleaner.clean(Jsoup.parseBodyFragment(aHtml, "")).body().html());
    }

    // mutant: Safelist#Safelist 把 深拷贝配置 改成 共享内部集合引用
    @Test public void safelistCopyIsIsolatedFromLaterMutation() {
        Safelist safelist = Safelist.basic();
        Safelist copy = new Safelist(safelist);
        Cleaner copyCleaner = new Cleaner(copy);
        String imgHtml = "<img src='http://e.com/i.png'>";

        safelist.addTags("img").addAttributes("img", "src").addProtocols("img", "src", "http", "https");
        assertEquals("", copyCleaner.clean(Jsoup.parseBodyFragment(imgHtml, "")).body().html());

        Safelist protoList = Safelist.basic();
        Safelist protoCopy = new Safelist(protoList);
        Cleaner protoCopyCleaner = new Cleaner(protoCopy);
        String aHtml = "<a href='http://e.com/'>x</a>";

        protoList.removeProtocols("a", "href", "http");
        assertEquals("<a href=\"http://e.com/\" rel=\"nofollow\">x</a>",
            protoCopyCleaner.clean(Jsoup.parseBodyFragment(aHtml, "")).body().html());
    }
}
