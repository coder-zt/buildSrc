package com.quicksdk.tools.utils


class TestHandlerXml{

    static void test() {
        File xmlFile = new File("E:\\ChannelCode0\\buildSrc\\src\\assets\\test_handler.xml")
        def xmlSlurper = new XmlSlurper()
        def response = xmlSlurper.parse(xmlFile)
//        response.
    }
}