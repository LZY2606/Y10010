package org.jsoup.safety;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 Boundary characterization tests for the {@link Cleaner} / {@link Safelist} sanitization surface.
 Every expected value was captured empirically against the current implementation; these pin
 divergent behaviour across the public entry points so that later changes are caught.
 */
public class SanitizeBoundaryTest {
    private static final String LinkHtml = "<a href='/foo'>Link</a>";
    private static final String ImgHtml = "<img src='/i.png'>";

    private void assertMatrix(String html, Safelist safelist,
                              String e1clean, String e2clean, String e3clean, String e4clean,
                              boolean e5valid, boolean e6valid, boolean e7valid) {
        assertEquals(e1clean, Jsoup.clean(html, safelist), "entry1 Jsoup.clean(String, Safelist)");

        assertEquals(e2clean, Jsoup.clean(html, "", safelist), "entry2 Jsoup.clean(String, empty baseUri, Safelist)");

        assertEquals(e3clean, Jsoup.clean(html, "", safelist, new OutputSettings()),
            "entry3 Jsoup.clean(String, empty baseUri, Safelist, OutputSettings)");

        Document dirty = Jsoup.parseBodyFragment(html, "");
        assertEquals(e4clean, new Cleaner(safelist).clean(dirty).body().html(),
            "entry4 new Cleaner(safelist).clean(parseBodyFragment(html, \"\"))");

        assertEquals(e5valid, Jsoup.isValid(html, safelist), "entry5 Jsoup.isValid(String, Safelist)");

        assertEquals(e6valid, new Cleaner(safelist).isValid(Jsoup.parseBodyFragment(html, "")),
            "entry6 new Cleaner(safelist).isValid(doc)");

        assertEquals(e7valid, new Cleaner(safelist).isValidBodyHtml(html),
            "entry7 new Cleaner(safelist).isValidBodyHtml(String)");
    }

    // mutant: Jsoup#clean 把 baseUri 空串下 preserveRelativeLinks(true) 时的 DummyUri 占位改成空串
    @Test public void matrix_basic_relative_defaultFalse_link() {
        assertMatrix(LinkHtml, Safelist.basic(),
            "<a rel=\"nofollow\">Link</a>",
            "<a rel=\"nofollow\">Link</a>",
            "<a rel=\"nofollow\">Link</a>",
            "<a rel=\"nofollow\">Link</a>",
            false, false, false);
    }

    // mutant: Jsoup#clean 把 baseUri 空串下 preserveRelativeLinks(true) 时的 DummyUri 占位改成空串
    @Test public void matrix_basic_relative_defaultFalse_img() {
        assertMatrix(ImgHtml, Safelist.basic(),
            "", "", "", "",
            false, false, false);
    }

    // mutant: Jsoup#clean 把 baseUri 空串下 preserveRelativeLinks(true) 时的 DummyUri 占位改成空串
    @Test public void matrix_basicWithImages_relative_defaultFalse_link() {
        assertMatrix(LinkHtml, Safelist.basicWithImages(),
            "<a rel=\"nofollow\">Link</a>",
            "<a rel=\"nofollow\">Link</a>",
            "<a rel=\"nofollow\">Link</a>",
            "<a rel=\"nofollow\">Link</a>",
            false, false, false);
    }

    // mutant: Jsoup#clean 把 baseUri 空串下 preserveRelativeLinks(true) 时的 DummyUri 占位改成空串
    @Test public void matrix_basicWithImages_relative_defaultFalse_img() {
        assertMatrix(ImgHtml, Safelist.basicWithImages(),
            "<img>", "<img>", "<img>", "<img>",
            false, false, false);
    }

    // mutant: Cleaner#createSafeElement 把 shouldAbsUrl 解析失败时回填原始属性值改成丢弃
    @Test public void matrix_basic_relative_preservedTrue_link() {
        assertMatrix(LinkHtml, Safelist.basic().preserveRelativeLinks(true),
            "<a href=\"/foo\">Link</a>",
            "<a href=\"/foo\">Link</a>",
            "<a rel=\"nofollow\">Link</a>",
            "<a rel=\"nofollow\">Link</a>",
            true, false, true);
    }

    // mutant: Safelist#isSafeTag 把 img 的 TagName 匹配从忽略大小写改成精确匹配
    @Test public void matrix_basic_relative_preservedTrue_img() {
        assertMatrix(ImgHtml, Safelist.basic().preserveRelativeLinks(true),
            "", "", "", "",
            false, false, false);
    }

    // mutant: Cleaner#createSafeElement 把 shouldAbsUrl 解析失败时回填原始属性值改成丢弃
    @Test public void matrix_basicWithImages_relative_preservedTrue_link() {
        assertMatrix(LinkHtml, Safelist.basicWithImages().preserveRelativeLinks(true),
            "<a href=\"/foo\">Link</a>",
            "<a href=\"/foo\">Link</a>",
            "<a rel=\"nofollow\">Link</a>",
            "<a rel=\"nofollow\">Link</a>",
            true, false, true);
    }

    // mutant: Cleaner#isValidBodyHtml 把 preserveRelativeLinks 时的 DummyUri 分支改成空串
    @Test public void matrix_basicWithImages_relative_preservedTrue_img() {
        assertMatrix(ImgHtml, Safelist.basicWithImages().preserveRelativeLinks(true),
            "<img src=\"/i.png\">",
            "<img src=\"/i.png\">",
            "<img>",
            "<img>",
            true, false, true);
    }

    // mutant: Jsoup#clean 把带 OutputSettings 的重载也加上 DummyUri 占位 (entry3 当前与 entry1/entry2 不一致)
    @Test public void divergence_outputSettingsOverloadDropsRelativeLinkAndRel() {
        Safelist safelist = Safelist.basicWithImages().preserveRelativeLinks(true);

        assertEquals("<a href=\"/foo\">Link</a>", Jsoup.clean(LinkHtml, "", safelist));
        assertEquals("<a rel=\"nofollow\">Link</a>",
            Jsoup.clean(LinkHtml, "", safelist, new OutputSettings()));

        assertEquals("<img src=\"/i.png\">", Jsoup.clean(ImgHtml, "", safelist));
        assertEquals("<img>", Jsoup.clean(ImgHtml, "", safelist, new OutputSettings()));
    }

    // mutant: Cleaner#clean 把内部 Safelist 的 preserveRelativeLinks 也当作 DummyUri 生效 (entry4 当前保留不了相对链接)
    @Test public void divergence_cleanerCleanIgnoresPreservedRelativeLinkWithoutBaseUri() {
        Safelist safelist = Safelist.basicWithImages().preserveRelativeLinks(true);
        Document doc = Jsoup.parseBodyFragment(LinkHtml, "");

        assertEquals("<a href=\"/foo\">Link</a>", Jsoup.clean(LinkHtml, safelist));
        assertEquals("<a rel=\"nofollow\">Link</a>", new Cleaner(safelist).clean(doc).body().html());

        Document imgDoc = Jsoup.parseBodyFragment(ImgHtml, "");
        assertEquals("<img src=\"/i.png\">", Jsoup.clean(ImgHtml, safelist));
        assertEquals("<img>", new Cleaner(safelist).clean(imgDoc).body().html());
    }

    // mutant: Cleaner#isValid 把入参文档的空 baseUri 也按 preserveRelativeLinks 视作 DummyUri (entry6 当前判 false)
    @Test public void divergence_cleanerIsValidRejectsRelativeLinkThatJsoupIsValidAccepts() {
        Safelist safelist = Safelist.basicWithImages().preserveRelativeLinks(true);

        assertTrue(Jsoup.isValid(LinkHtml, safelist));
        assertFalse(new Cleaner(safelist).isValid(Jsoup.parseBodyFragment(LinkHtml, "")));

        assertTrue(Jsoup.isValid(ImgHtml, safelist));
        assertFalse(new Cleaner(safelist).isValid(Jsoup.parseBodyFragment(ImgHtml, "")));
    }

    // mutant: Safelist#isSafeProtocol 把 scheme 后面必须紧跟冒号的 charAt 判定删掉
    @ParameterizedTest
    @CsvSource({"httpx://example.com", "httpsfoo", "https-x://e.com"})
    public void schemePrefixWithoutColonIsDropped(String href) {
        Safelist safelist = Safelist.basic();
        String html = "<a href='" + href + "'>x</a>";

        assertEquals("<a rel=\"nofollow\">x</a>", Jsoup.clean(html, safelist));
        assertFalse(Jsoup.isValid(html, safelist));
    }

    // mutant: Safelist#isSafeProtocol 把 lowerCase(value) 判定改成原始大小写
    @Test public void mixedCaseSchemeLowercasedButHostCasePreserved() {
        Safelist safelist = Safelist.basic();
        String html = "<a href='HTTP://Example.COM/A'>x</a>";

        assertEquals("<a href=\"http://Example.COM/A\" rel=\"nofollow\">x</a>", Jsoup.clean(html, safelist));
        assertTrue(Jsoup.isValid(html, safelist));
    }

    // mutant: Safelist#isValidAnchor 把以 # 开头的判定去掉
    @Test public void hashProtocolAcceptsPlainAnchorAndKeepsRel() {
        Safelist safelist = Safelist.basic().addProtocols("a", "href", "#");
        String html = "<a href='#foo'>x</a>";

        assertEquals("<a href=\"#foo\" rel=\"nofollow\">x</a>", Jsoup.clean(html, safelist));
        assertTrue(Jsoup.isValid(html, safelist));
    }

    // mutant: Safelist#isValidAnchor 把空白正则 \\s 的判定去掉
    @Test public void hashProtocolRejectsAnchorWithSpace() {
        Safelist safelist = Safelist.basic().addProtocols("a", "href", "#");
        String html = "<a href='#foo bar'>x</a>";

        assertEquals("<a rel=\"nofollow\">x</a>", Jsoup.clean(html, safelist));
        assertFalse(Jsoup.isValid(html, safelist));
    }

    // mutant: Safelist#isValidAnchor 把空白正则 \\s 的判定去掉
    @Test public void hashProtocolRejectsAnchorWithTab() {
        Safelist safelist = Safelist.basic().addProtocols("a", "href", "#");
        String html = "<a href='#foo\tbar'>x</a>";

        assertEquals("<a rel=\"nofollow\">x</a>", Jsoup.clean(html, safelist));
        assertFalse(Jsoup.isValid(html, safelist));
    }

    // mutant: Safelist#isValidAnchor 把空白正则 \\s 的判定去掉
    @Test public void hashProtocolRejectsAnchorWithNewline() {
        Safelist safelist = Safelist.basic().addProtocols("a", "href", "#");
        String html = "<a href='#foo\nbar'>x</a>";

        assertEquals("<a rel=\"nofollow\">x</a>", Jsoup.clean(html, safelist));
        assertFalse(Jsoup.isValid(html, safelist));
    }

    // mutant: Cleaner#createSafeElement 把 enforced 属性先 removeIgnoreCase 再 put 的折叠逻辑改成保留原键
    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
        "rel=\"nofollow\"|true",
        "rel=\"NOFOLLOW\"|false",
        "REL=\"nofollow\"|true",
        "REL=\"x\"|false"
    })
    public void enforcedRelFoldsKeyAndValueAndKeepsPositionAfterHref(String sourceAttr, boolean expectedValid) {
        Safelist safelist = Safelist.basic().addEnforcedAttribute("a", "rel", "nofollow");
        String html = "<a href='http://example.com/' " + sourceAttr + ">L</a>";

        String expected = "<a href=\"http://example.com/\" rel=\"nofollow\">L</a>";
        assertEquals(expected, Jsoup.clean(html, safelist));
        assertEquals(expectedValid, Jsoup.isValid(html, safelist));

        Element cleaned = new Cleaner(safelist).clean(Jsoup.parseBodyFragment(html, "")).selectFirst("a");
        assertEquals(2, cleaned.attributes().size());
        Attribute first = cleaned.attributes().asList().get(0);
        Attribute second = cleaned.attributes().asList().get(1);
        assertEquals("href", first.getKey());
        assertEquals("http://example.com/", first.getValue());
        assertEquals("rel", second.getKey());
        assertEquals("nofollow", second.getValue());
    }

    // mutant: Cleaner#createSafeElement 把 enforced 属性追加位置改到已保留源属性之前
    @Test public void allowedAttributeOrderHrefThenTitleWithRelAppended() {
        Safelist safelist = Safelist.basic().addAttributes("a", "title");
        String html = "<a href='http://example.com/a' title='T'>L</a>";

        assertEquals("<a href=\"http://example.com/a\" title=\"T\" rel=\"nofollow\">L</a>", Jsoup.clean(html, safelist));

        Element cleaned = new Cleaner(safelist).clean(Jsoup.parseBodyFragment(html, "")).selectFirst("a");
        assertEquals("href", cleaned.attributes().asList().get(0).getKey());
        assertEquals("title", cleaned.attributes().asList().get(1).getKey());
        assertEquals("rel", cleaned.attributes().asList().get(2).getKey());
    }

    // mutant: Cleaner#createSafeElement 把源属性遍历改成按键排序而不是保留文档顺序
    @Test public void allowedAttributeOrderTitleThenHrefKeptWithRelAppended() {
        Safelist safelist = Safelist.basic().addAttributes("a", "title");
        String html = "<a title='T' href='http://example.com/a'>L</a>";

        assertEquals("<a title=\"T\" href=\"http://example.com/a\" rel=\"nofollow\">L</a>", Jsoup.clean(html, safelist));

        Element cleaned = new Cleaner(safelist).clean(Jsoup.parseBodyFragment(html, "")).selectFirst("a");
        assertEquals("title", cleaned.attributes().asList().get(0).getKey());
        assertEquals("href", cleaned.attributes().asList().get(1).getKey());
        assertEquals("rel", cleaned.attributes().asList().get(2).getKey());
    }

    // mutant: Cleaner#isValid 把 copySafeNodes 的丢弃计数口径改成把遍历起点 body 也算一个
    @Test public void isValidBodyRootElementIsNotCountedAsDiscarded() {
        Safelist safelist = Safelist.basic();
        Cleaner cleaner = new Cleaner(safelist);

        Document shell = Document.createShell("");
        assertTrue(cleaner.isValid(shell));
        assertTrue(cleaner.isValidBodyHtml(""));
    }

    // mutant: Cleaner#isValid 把 dirtyDocument.head().childNodes().isEmpty() 这一项从判定中删掉
    @Test public void isValidFalseWhenHeadHasContent() {
        Safelist safelist = Safelist.basic();
        Cleaner cleaner = new Cleaner(safelist);

        Document withHead = Jsoup.parse("<title>T</title>");
        assertFalse(cleaner.isValid(withHead));
        assertFalse(cleaner.isValidBodyHtml("<title>T</title>"));
    }

    // mutant: Cleaner#isValid 把 numDiscarded == 0 的判定改成 >= 0
    @Test public void isValidFalseWhenBodyContainsOnlyComment() {
        Safelist safelist = Safelist.basic();
        Cleaner cleaner = new Cleaner(safelist);

        Document commentDoc = Jsoup.parseBodyFragment("<!-- c -->", "");
        assertFalse(cleaner.isValid(commentDoc));
        assertFalse(cleaner.isValidBodyHtml("<!-- c -->"));
        assertEquals("", Jsoup.clean("<!-- c -->", safelist));
    }

    // mutant: Cleaner#isValid 把 TextNode 不计丢弃改成计为丢弃
    @Test public void isValidTrueWhenBodyContainsOnlyText() {
        Safelist safelist = Safelist.basic();
        Cleaner cleaner = new Cleaner(safelist);

        Document textDoc = Jsoup.parseBodyFragment("hello", "");
        assertTrue(cleaner.isValid(textDoc));
        assertTrue(cleaner.isValidBodyHtml("hello"));
        assertEquals("hello", Jsoup.clean("hello", safelist));
    }

    // mutant: Cleaner#isValid 删掉 head 非空检查后, isValid(Document) 与 isValidBodyHtml 对同语义输入的分歧就消失
    @Test public void isValidAndIsValidBodyHtmlDivergeOnHeadContent() {
        Safelist safelist = Safelist.basic();
        Cleaner cleaner = new Cleaner(safelist);

        Document fullDoc = Jsoup.parse("<title>T</title><p>Hi</p>");
        assertFalse(cleaner.isValid(fullDoc));
        assertTrue(cleaner.isValidBodyHtml("<p>Hi</p>"));
        assertFalse(cleaner.isValidBodyHtml("<title>T</title><p>Hi</p>"));
    }

    // mutant: Safelist#addTags 把标签加入的 tagNames 改成 Cleaner 构造时被快照一份
    @Test public void lifecycleAddTagsVisibleToExistingCleaner() {
        Safelist safelist = Safelist.basic();
        Cleaner cleaner = new Cleaner(safelist);

        String html = "<div>Hi</div>";
        assertEquals("Hi", cleaner.clean(Jsoup.parseBodyFragment(html, "")).body().html());

        safelist.addTags("div");

        assertEquals("<div>Hi</div>", cleaner.clean(Jsoup.parseBodyFragment(html, "")).body().html());
        assertTrue(cleaner.isValid(Jsoup.parseBodyFragment(html, "")));
    }

    // mutant: Safelist#removeProtocols 把协议从 Set 移除改成对已构造的 Cleaner 不再生效
    @Test public void lifecycleRemoveProtocolsVisibleToExistingCleaner() {
        Safelist safelist = Safelist.basic();
        Cleaner cleaner = new Cleaner(safelist);

        String html = "<a href='ftp://x.com/'>x</a>";
        assertEquals("<a href=\"ftp://x.com/\" rel=\"nofollow\">x</a>",
            cleaner.clean(Jsoup.parseBodyFragment(html, "")).body().html());

        safelist.removeProtocols("a", "href", "ftp");

        assertEquals("<a rel=\"nofollow\">x</a>",
            cleaner.clean(Jsoup.parseBodyFragment(html, "")).body().html());
        assertFalse(Jsoup.isValid(html, safelist));
    }

    // mutant: Safelist#Safelist(Safelist) 把深拷贝改成与原对象共享同一个 tagNames/protocols 集合
    @Test public void lifecycleCopiedSafelistUnaffectedByLaterMutations() {
        Safelist original = Safelist.basic();
        Safelist copy = new Safelist(original);
        Cleaner originalCleaner = new Cleaner(original);
        Cleaner copyCleaner = new Cleaner(copy);

        original.addTags("div");
        assertEquals("<div>Hi</div>", originalCleaner.clean(Jsoup.parseBodyFragment("<div>Hi</div>", "")).body().html());
        assertEquals("Hi", copyCleaner.clean(Jsoup.parseBodyFragment("<div>Hi</div>", "")).body().html());

        original.removeProtocols("a", "href", "http");
        assertEquals("<a rel=\"nofollow\">x</a>",
            originalCleaner.clean(Jsoup.parseBodyFragment("<a href='http://e.com'>x</a>", "")).body().html());
        assertEquals("<a href=\"http://e.com\" rel=\"nofollow\">x</a>",
            copyCleaner.clean(Jsoup.parseBodyFragment("<a href='http://e.com'>x</a>", "")).body().html());
    }
}
