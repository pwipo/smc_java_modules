package ru.smcsystem.smcmodules.module;

import org.junit.Test;
import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.api.enumeration.MessageType;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.*;

public class WindowTest {

    @Test
    public void process() throws InterruptedException {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "mode", new Value("PASSIVE"),
                                "getPressedKeys", new Value("true"),
                                "printElements", new Value("true"),
                                "sleep", new Value(1000),
                                "useButtonEvents", new Value("true"),
                                "useMenuEvents", new Value("true"),
                                "useListSelectionEvents", new Value("true"),
                                "useTreeSelectionEvents", new Value("true"),
                                "defaultButton", new Value(""),
                                "configuration", new Value("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                        "<frame\n" +
                                        "        xmlns=\"http://www.swixml.org/2007/SwixmlTags\"\n" +
                                        "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                        "        xsi:schemaLocation=\"http://www.swixml.org/2007/SwixmlTags http://www.swixml.org/2007/swixml.xsd\"\n" +
                                        "        size=\"640,280\" title=\"[[title]]\" defaultCloseOperation=\"JFrame.EXIT_ON_CLOSE\">\n" +
                                        "\n" +
                                        "    <layout type=\"borderlayout\"/>\n" +
                                        "\n" +
                                        "    <panel constraints=\"BorderLayout.NORTH\">\n" +
                                        "        <label font=\"Georgia-BOLD-36\" text=\"[[north]]\"/>\n" +
                                        "        <textfield id=\"northInput\" columns=\"20\" Text=\"[[north]]\"/>\n" +
                                        "    </panel>\n" +
                                        "\n" +
                                        "    <panel constraints=\"BorderLayout.CENTER\">\n" +
                                        "        <label font=\"Georgia-BOLD-36\" text=\"[[center]]\"/>\n" +
                                        "        <textfield id=\"centerhInput\" columns=\"20\" Text=\"[[center]]\"/>\n" +
                                        "    </panel>\n" +
                                        "\n" +
                                        "    <panel constraints=\"BorderLayout.SOUTH\">\n" +
                                        "        <label font=\"Georgia-BOLD-36\" text=\"[[south]]\"/>\n" +
                                        "        <textfield id=\"southInput\" columns=\"20\" Text=\"[[south]]\"/>\n" +
                                        "        <button id=\"buttonSouth\" text=\"[[button]]\"/>\n" +
                                        "    </panel>\n" +
                                        "\n" +
                                        "</frame>")

                        ),
                        null,
                        null
                ),
                new Window()
        );
        process.getConfigurationTool().getAllSettings().put(
                "lang", new Value("north::en::NORTH::ru::Север;;" +
                        "center::en::CENTER::ru::Центор;;" +
                        "south::en::SOUTH::ru::Юг;;" +
                        "button::en::BUTTON::ru::Кнопка;;" +
                        "title::en::Text::ru::Текст"
                )
        );

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null, null, null);
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        Thread.sleep(5000);

        ExecutionContextToolImpl executionContextTool2 = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(1)),
                                                new Message(MessageType.DATA, new Date(), new Value("centerhInput"))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
        process.execute(executionContextTool2);
        executionContextTool2.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool2.getOutput().clear();

        Thread.sleep(5000);

        ExecutionContextToolImpl executionContextTool3 = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("centerhInput")),
                                                new Message(MessageType.DATA, new Date(), new Value("test1"))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
        process.execute(executionContextTool3);
        executionContextTool3.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool3.getOutput().clear();

        Thread.sleep(5000);

        ExecutionContextToolImpl executionContextTool4 = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(0))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
        process.execute(executionContextTool);
        process.execute(executionContextTool4);
        executionContextTool4.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool4.getOutput().clear();

        process.stop();
    }

    @Test
    public void process2() throws InterruptedException {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "mode", new Value("ACTIVE"),
                                "getPressedKeys", new Value("true"),
                                "printElements", new Value("true"),
                                "sleep", new Value(1000),
                                "useButtonEvents", new Value("true"),
                                "useMenuEvents", new Value("true"),
                                "useListSelectionEvents", new Value("true"),
                                "useTreeSelectionEvents", new Value("true"),
                                "defaultButton", new Value(""),
                                "configuration", new Value("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                        "<frame\n" +
                                        "        xmlns=\"http://www.swixml.org/2007/SwixmlTags\"\n" +
                                        "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                        "        xsi:schemaLocation=\"http://www.swixml.org/2007/SwixmlTags http://www.swixml.org/2007/swixml.xsd\"\n" +
                                        "        size=\"640,280\" title=\"Test\" defaultCloseOperation=\"JFrame.EXIT_ON_CLOSE\">\n" +
                                        "\n" +
                                        "    <layout type=\"borderlayout\"/>\n" +
                                        "\n" +
                                        "    <menubar id=\"mb\">\n" +
                                        "        <menu id=\"mu_file\" text=\"File\">\n" +
                                        "               <menuitem id=\"mi_new\" text=\"New\" mnemonic=\"VK_N\"/>\n" +
                                        "               <menuitem id=\"mi_open\" text=\"Open\" mnemonic=\"VK_O\"/>\n" +
                                        "               <menuitem id=\"mi_save_as\" text=\"Save as\" mnemonic=\"VK_S\" ActionCommand=\"AC_SAVE_AS\"/>\n" +
                                        "               <separator/>\n" +
                                        "               <menuitem id=\"mi_exit\" text=\"Exit\" mnemonic=\"VK_X\" ActionCommand=\"AC_EXIT\"/>\n" +
                                        "        </menu>\n" +
                                        "    </menubar>\n" +
                                        "\n" +
                                        "    <panel constraints=\"BorderLayout.NORTH\">\n" +
                                        "        <label font=\"Georgia-BOLD-36\" text=\"NORTH\"/>\n" +
                                        "        <textfield id=\"northInput\" columns=\"20\" Text=\"NORTH\"/>\n" +
                                        "    </panel>\n" +
                                        "\n" +
                                        "    <panel constraints=\"BorderLayout.CENTER\">\n" +
                                        "        <label font=\"Georgia-BOLD-36\" text=\"CENTER\"/>\n" +
                                        "        <textfield id=\"centerhInput\" columns=\"20\" Text=\"CENTER\"/>\n" +
                                        "        <scrollpane>\n" +
                                        "           <list id=\"mList\" Font=\"ARIAL-BOLD-14\" VisibleRowCount=\"3\"/>\n" +
                                        "         </scrollpane>\n" +
                                        "    </panel>\n" +
                                        "\n" +
                                        "    <panel constraints=\"BorderLayout.SOUTH\">\n" +
                                        "        <label font=\"Georgia-BOLD-36\" text=\"SOUTH\"/>\n" +
                                        "        <textfield id=\"southInput\" columns=\"20\" Text=\"SOUTH\"/>\n" +
                                        "        <button id=\"buttonSouth\" text=\"Center button\"/>\n" +
                                        "    </panel>\n" +
                                        "\n" +
                                        "</frame>")
                        ),
                        null,
                        null
                ),
                new Window()
        );
        process.getConfigurationTool().getAllSettings().put("lang", new Value(""));

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null, null, null,
                List.of(
                        args -> {
                            System.out.println(args);
                            return new Action(List.of(), ActionType.EXECUTE
                            );
                        }
                ));

        new Thread(() -> {
            process.execute(executionContextTool);
            executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
            executionContextTool.getOutput().clear();
        }).start();

        Thread.sleep(1000);

        ExecutionContextToolImpl executionContextTool2 = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(10)),
                                                new Message(MessageType.DATA, new Date(), new Value("mList")),
                                                new Message(MessageType.DATA, new Date(), new Value("one")),
                                                new Message(MessageType.DATA, new Date(), new Value("two")),
                                                new Message(MessageType.DATA, new Date(), new Value("three")),
                                                new Message(MessageType.DATA, new Date(), new Value("four"))
                                        ),
                                        ActionType.EXECUTE
                                )
                        )
                ), null, null);
        process.execute(executionContextTool2);
        executionContextTool2.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool2.getOutput().clear();

        Thread.sleep(10000);

        process.stop();
    }

    @Test
    public void process3() throws InterruptedException {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "mode", new Value("ACTIVE"),
                                "getPressedKeys", new Value("true"),
                                "printElements", new Value("true"),
                                "sleep", new Value(1000),
                                "useButtonEvents", new Value("true"),
                                "useMenuEvents", new Value("true"),
                                "useListSelectionEvents", new Value("true"),
                                "useTreeSelectionEvents", new Value("true"),
                                "defaultButton", new Value(""),
                                "configuration", new Value("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                        "<frame\n" +
                                        "\t\txmlns=\"http://www.swixml.org/2007/SwixmlTags\"\n" +
                                        "\t\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                        "\t\txsi:schemaLocation=\"http://www.swixml.org/2007/SwixmlTags http://www.swixml.org/2007/swixml.xsd\"\n" +
                                        "\t\tsize=\"800,600\" title=\"Mail Client\" defaultCloseOperation=\"JFrame.EXIT_ON_CLOSE\">\n" +
                                        "\n" +
                                        "\t<layout type=\"borderlayout\"/>\n" +
                                        "\n" +
                                        "\t<panel constraints=\"BorderLayout.NORTH\">\n" +
                                        "\t\t<button id=\"cmd_get_all\" text=\"Update\"/>\n" +
                                        "\t\t<button id=\"cmd_get_message\" text=\"Read\" Enable=\"false\"/>\n" +
                                        "\t\t<button id=\"cmd_delete_message\" text=\"Delete\" Enable=\"false\"/>\n" +
                                        "\t\t<button id=\"cmd_new\" text=\"New\"/>\n" +
                                        "\t\t<button id=\"cmd_sent_new\" text=\"Sent\" Enable=\"false\"/>\n" +
                                        "\t</panel>\n" +
                                        "\n" +
                                        "\t<panel constraints=\"BorderLayout.WEST\" layout=\"borderlayout\">\n" +
                                        "\t\t<label font=\"Georgia-BOLD-36\" text=\"Messages\" constraints=\"BorderLayout.NORTH\"/>\n" +
                                        "\t\t<scrollpane constraints=\"BorderLayout.CENTER\">\n" +
                                        "\t\t\t<list id=\"list\" Font=\"ARIAL-BOLD-14\" VisibleRowCount=\"10\"/>\n" +
                                        "\t\t</scrollpane>\n" +
                                        "\t</panel>\n" +
                                        "\n" +
                                        "\t<scrollpane constraints=\"BorderLayout.CENTER\">\n" +
                                        "\t\t<TextPane id=\"text\" editable=\"false\" Text=\" \" ContentType=\"text/html\" />\n" +
                                        "\t</scrollpane>\n" +
                                        "\n" +
                                        "</frame>\n")
                        ),
                        null,
                        null
                ),
                new Window()
        );
        process.getConfigurationTool().getAllSettings().put("lang", new Value(""));

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                null,
                null,
                null,
                List.of(
                        args -> {
                            System.out.println(args);
                            return new Action(List.of(), ActionType.EXECUTE
                            );
                        }
                ));

        new Thread(() -> {
            process.execute(executionContextTool);
            executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
            executionContextTool.getOutput().clear();
        }).start();

        Thread.sleep(1000);

        ExecutionContextToolImpl executionContextTool2 = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("text")),
                                                new Message(MessageType.DATA, new Date(), new Value("" +
                                                        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
                                                        "<html>\n" +
                                                        "<head>\n" +
                                                        "<title>C Новым Годом!</title>\n" +
                                                        "<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>\n" +
                                                        "<style type=\"text/css\">\n" +
                                                        "@media (prefers-color-scheme:  light) {\n" +
                                                        "html {\n" +
                                                        "background: #dcdcdc !important;\n" +
                                                        "font-color: #191919 !important;\n" +
                                                        "}\n" +
                                                        "}\n" +
                                                        "@media (prefers-color-scheme: dark) {\n" +
                                                        "html {\n" +
                                                        "background: #dcdcdc !important;\n" +
                                                        "font-color: #191919 !important;\n" +
                                                        "}\n" +
                                                        "}\n" +
                                                        "</style>\n" +
                                                        "<style>\n" +
                                                        "*{padding:0px;}\n" +
                                                        "html{height:100%;}\n" +
                                                        "body {margin:0px;padding:0px;background-color:#dcdcdc;height:100%;\n" +
                                                        "background-image:url('http://www.rsb.ru/f/1/global/i/CRM/rsb/19052019/2px_grey.jpg');\n" +
                                                        "background-repeat:repeat;\n" +
                                                        "}\n" +
                                                        "#maincontent{\n" +
                                                        "background-image:url('http://www.rsb.ru/f/1/global/i/CRM/rsb/19052019/2px_grey2.jpg');\n" +
                                                        "background-repeat:repeat;\n" +
                                                        "}\n" +
                                                        "p, img{padding:0px;margin:0px;font-family:Arial;}\n" +
                                                        "li{font-size:17px;}\n" +
                                                        ".reg{line-height: 0px; font-size: 80%;}\n" +
                                                        "a{color:#861F7F;}\n" +
                                                        "a:hover{color:#FF0000;}\n" +
                                                        "table{max-width:650px}\n" +
                                                        "p {\n" +
                                                        "font-family:Georgia, serif;\n" +
                                                        "font-size:79%;\n" +
                                                        "line-height:1.6;\n" +
                                                        "}\n" +
                                                        "ul li{\n" +
                                                        "margin:0px 0px 0px 70px;\n" +
                                                        "font-size:14px;\n" +
                                                        "font-family:Arial;\n" +
                                                        "text-align:left;\n" +
                                                        "line-height:20px;\n" +
                                                        "color:#515151;\n" +
                                                        "}\n" +
                                                        "</style>\n" +
                                                        "</head>\n" +
                                                        "<body bgcolor=\"#dcdcdc\" style=\"background-color:#dcdcdc\">\n" +
                                                        "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\" width=\"650px\" bgcolor=\"#dcdcdc\" style=\"background-color:#dcdcdc\">\n" +
                                                        "<tr><td align=\"center\" valign=\"top\" bgcolor=\"#f7f9f8\" style=\"background-color:#f7f9f8\">\n" +
                                                        "\n" +
                                                        "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"650px\" style=\"width:650px\" align=\"center\" id=\"maincontent\">\n" +
                                                        "\n" +
                                                        "<tr>\n" +
                                                        "<td width=\"650px\" style=\"width:650px\">\n" +
                                                        "\n" +
                                                        "\n" +
                                                        "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"650px\" style=\"width:650px\">\n" +
                                                        "\n" +
                                                        "<tr>\n" +
                                                        "<td colspan=\"3\" align=\"center\">\n" +
                                                        "\n" +
                                                        "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"580px\" style=\"width:580px\">\n" +
                                                        "<tr>\n" +
                                                        "<td colspan=\"5\" align=\"center\">\n" +
                                                        "\n" +
                                                        "\n" +
                                                        "<img src=\"http://www.rsb.ru/f/1/global/i/CRM/rsb/20191212-NY/Bnr_NY2020_580-5.gif\" style=\"display:block\" border=\"0\"/>\n" +
                                                        "\n" +
                                                        "</td>\n" +
                                                        "</tr>\n" +
                                                        "</table>\n" +
                                                        "\n" +
                                                        "</td>\n" +
                                                        "</tr>\n" +
                                                        "\n" +
                                                        "<tr>\n" +
                                                        "<td colspan=\"3\" style=\"background-color:#f7f9f8;\" bgcolor=\"#f7f9f8\" align=\"center\">\n" +
                                                        "<br/>\n" +
                                                        "<p style=\"width:575px;margin:0px;font-size:18px;font-family:Arial;text-align:center;line-height:1.4em;color:#4f4f4f;margin-top:20px;margin-bottom:0px;\">\n" +
                                                        "<b><span style=\"color:#f31b1e\">Уважаемый НИКОЛАЙ&nbsp;ВЛАДИМИРОВИЧ!</span></b><br /><br />\n" +
                                                        "Желаем Вам уюта в доме, радости в душе, любви в сердце.<br />\n" +
                                                        "Будьте счастливы, здоровы и жизнерадостны.<br />\n" +
                                                        "Мира и благополучия в наступающем году!</p>\n" +
                                                        "\n" +
                                                        "<p style=\"margin:0px;font-size:20px;line-height:20px\">&nbsp;</p>\n" +
                                                        "<img src=\"http://www.rsb.ru/f/1/global/i/CRM/rsb/20191212-NY/balls.png\" style=\"display:block\" border=\"0\"/>\n" +
                                                        "\n" +
                                                        "\n" +
                                                        "\n" +
                                                        "\n" +
                                                        "<p style=\"width:650px;margin:0px;font-size:15px;font-family:Arial;text-align:center;line-height:19px;color:#4f4f4f;margin-top:20px;margin-bottom:0px;margin-left:0px;margin-right:0px;\">\n" +
                                                        "Ваш Банк Русский Стандарт.\n" +
                                                        "</p>\n" +
                                                        "\n" +
                                                        "\n" +
                                                        "<br/><br/>\n" +
                                                        "\n" +
                                                        "</td>\n" +
                                                        "</tr>\n" +
                                                        "\n" +
                                                        "\n" +
                                                        "\n" +
                                                        "</table>\n" +
                                                        "\n" +
                                                        "</td>\n" +
                                                        "</tr>\n" +
                                                        "</table>\n" +
                                                        "\n" +
                                                        "</td>\n" +
                                                        "</tr>\n" +
                                                        "\n" +
                                                        "<tr>\n" +
                                                        "<td align=\"center\">\n" +
                                                        "\n" +
                                                        "<p style=\"margin:0px;font-size:30px;line-height:30px\">&nbsp;</p>\n" +
                                                        "\n" +
                                                        "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\" width=\"235px\" style=\"width:235px\">\n" +
                                                        "<tr>\n" +
                                                        "<td><a href=\"http://www.rsb.ru/_loader.html?send_id=U82&user_id=88892606864824654&url=https%3A%2F%2Fwww.facebook.com%2Frsbank\" target=\"_blank\"><img src=\"http://www.rsb.ru/f/1/global/i/CRM/rsb/12112019/soc1.png\" style=\"display:block\" border=\"0\"/></a></td>\n" +
                                                        "<td><a href=\"http://www.rsb.ru/_loader.html?send_id=U82&user_id=88892606864824654&url=http%3A%2F%2Fwww.ok.ru%2Frsb\" target=\"_blank\"><img src=\"http://www.rsb.ru/f/1/global/i/CRM/rsb/12112019/soc2.png\" style=\"display:block\" border=\"0\"/></a></td>\n" +
                                                        "<td><a href=\"http://www.rsb.ru/_loader.html?send_id=U82&user_id=88892606864824654&url=http%3A%2F%2Fvk.com%2Frsb\" target=\"_blank\"><img src=\"http://www.rsb.ru/f/1/global/i/CRM/rsb/12112019/soc3.png\" style=\"display:block\" border=\"0\"/></a></td>\n" +
                                                        "<td><a href=\"http://www.rsb.ru/_loader.html?send_id=U82&user_id=88892606864824654&url=https%3A%2F%2Ftwitter.com%2Fbank_rs\" target=\"_blank\"><img src=\"http://www.rsb.ru/f/1/global/i/CRM/rsb/12112019/soc4.png\" style=\"display:block\" border=\"0\"/></a></td>\n" +
                                                        "<td><a href=\"http://www.rsb.ru/_loader.html?send_id=U82&user_id=88892606864824654&url=https%3A%2F%2Fwww.youtube.com%2Frussianstandardbank\" target=\"_blank\"><img src=\"http://www.rsb.ru/f/1/global/i/CRM/rsb/12112019/soc5.png\" style=\"display:block\" border=\"0\"/></a></td>\n" +
                                                        "</tr>\n" +
                                                        "</table>\n" +
                                                        "\n" +
                                                        "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\" width=\"650px\" style=\"width:650px\">\n" +
                                                        "<tr>\n" +
                                                        "<td>\n" +
                                                        "<p style=\"width:550px;margin:0px;font-size:11px;font-family:Arial;text-align:left;line-height:14px;color:#515151;margin-top:30px;margin-bottom:0px;margin-left:50px;margin-right:50px;\">\n" +
                                                        "\n" +
                                                        "АО «Банк Русский Стандарт». Генеральная лицензия Банка России №2289 выдана бессрочно<br/>\n" +
                                                        "19 ноября 2014 года.\n" +
                                                        "<br/><br/>\n" +
                                                        "Если письмо некорректно отображается, смотрите <a href=\"http://www.rsb.ru/f/1/global/i/CRM/rsb/20191212-NY/HTML-NY.htm\" style=\"color:#515151;\">здесь</a> | <a href=\"https://anketa.rsb.ru/applications/subscribe/unsubscribe/U82/88892606864824654/%D0%9D%D0%98%D0%9A%D0%9E%D0%9B%D0%90%D0%99%20%D0%92%D0%9B%D0%90%D0%94%D0%98%D0%9C%D0%98%D0%A0%D0%9E%D0%92%D0%98%D0%A7/ulianownv@mail.ru\" target=\"_blank\" style=\"color:#515151;\">Отменить рассылку</a>\n" +
                                                        "</p>\n" +
                                                        "\n" +
                                                        "<br/><br/>\n" +
                                                        "\n" +
                                                        "</td>\n" +
                                                        "</tr>\n" +
                                                        "</table>\n" +
                                                        "\n" +
                                                        "</td>\n" +
                                                        "</tr>\n" +
                                                        "\n" +
                                                        "</table>\n" +
                                                        "\n" +
                                                        "<img src=\"http://www.rsb.ru/_loader.html?send_id=U82&user_id=88892606864824654\">\n" +
                                                        "\n" +
                                                        "</body>\n" +
                                                        "</html>"))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
        process.execute(executionContextTool2);
        executionContextTool2.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool2.getOutput().clear();

        Thread.sleep(10000);

        process.stop();
    }

    @Test
    public void process4() throws InterruptedException {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "mode", new Value("ACTIVE"),
                                "getPressedKeys", new Value("false"),
                                "printElements", new Value("false"),
                                "sleep", new Value(1000),
                                "useButtonEvents", new Value("true"),
                                "useMenuEvents", new Value("true"),
                                "useListSelectionEvents", new Value("true"),
                                "useTreeSelectionEvents", new Value("true"),
                                "defaultButton", new Value("cmd_calc"),
                                "configuration", new Value("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                        "<frame\n" +
                                        "\t\txmlns=\"http://www.swixml.org/2007/SwixmlTags\"\n" +
                                        "\t\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                        "\t\txsi:schemaLocation=\"http://www.swixml.org/2007/SwixmlTags http://www.swixml.org/2007/swixml.xsd\"\n" +
                                        "\t\tsize=\"640,340\" title=\"Calculator\" defaultCloseOperation=\"JFrame.EXIT_ON_CLOSE\">\n" +
                                        "\n" +
                                        "\t<layout type=\"flowlayout\"/>\n" +
                                        "\n" +
                                        "\t<scrollpane minimumSize=\"600, 200\" maximumSize=\"600, 200\">\n" +
                                        "\t\t<list id=\"list\" Font=\"ARIAL-BOLD-14\" FixedCellWidth=\"600\" VisibleRowCount=\"10\"/>\n" +
                                        "\t</scrollpane>\n" +
                                        "\t<scrollpane minimumSize=\"600, 200\" maximumSize=\"600, 200\">\n" +
                                        "\t\t<textarea id=\"text\" editable=\"true\" text=\"\" columns=\"60\" rows=\"5\" />\n" +
                                        "\t\t<!--<textarea id=\"input\" rows=\"10\" columns=\"77\" editable=\"true\" lineWrap=\"true\" wrapStyleWord=\"true\" text=\"5 5 5 5\"/>-->\n" +
                                        "\t</scrollpane>\n" +
                                        "\t<button id=\"cmd_help\" text=\"?\"/>\n" +
                                        "\t<button id=\"cmd_calc\" text=\"=\"/>\n" +
                                        "\n" +
                                        "</frame>")
                        ),
                        null,
                        null
                ),
                new Window()
        );
        process.getConfigurationTool().getAllSettings().put("lang", new Value(""));

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                null
                , null
                , null,
                List.of(
                        args -> {
                            System.out.println(args);
                            return new Action(
                                    List.of(
                                            new Message(MessageType.DATA, new Date(), new Value(11))
                                            , new Message(MessageType.DATA, new Date(), new Value("list"))
                                            , new Message(MessageType.DATA, new Date(), new Value("1+5"))
                                            , new Message(MessageType.DATA, new Date(), new Value(11))
                                            , new Message(MessageType.DATA, new Date(), new Value("list"))
                                            , new Message(MessageType.DATA, new Date(), new Value("6.0"))
                                    )
                                    , ActionType.EXECUTE
                            );
                        }
                )
                // , null
        );

        Thread thread = new Thread(() -> {
            process.execute(executionContextTool);
            executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
            executionContextTool.getOutput().clear();
        });
        thread.start();
        // process.execute(executionContextTool);

        Thread.sleep(1000);

        ExecutionContextToolImpl executionContextTool2 = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(11))
                                                , new Message(MessageType.DATA, new Date(), new Value("list"))
                                                , new Message(MessageType.DATA, new Date(), new Value("input text:"))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
        process.execute(executionContextTool2);
        executionContextTool2.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool2.getOutput().clear();

        // Thread.sleep(50000);
        thread.join();

        process.stop();
    }

    @Test
    public void processGui() throws InterruptedException {
        Map<String, IValue> settings = new HashMap<>(Map.of(
                "mode", new Value("ACTIVE"),
                "getPressedKeys", new Value("false"),
                "printElements", new Value("false"),
                "sleep", new Value(1000),
                "useButtonEvents", new Value("true"),
                "useMenuEvents", new Value("true"),
                "useListSelectionEvents", new Value("true"),
                "useTreeSelectionEvents", new Value("true"),
                "defaultButton", new Value(""),
                "configuration", new Value("")
        ));
        settings.put("shapeId", new Value("root"));
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        settings,
                        null,
                        null
                ),
                new Window()
        );
        process.getConfigurationTool().getAllSettings().put("lang", new Value(""));
        ((Container) process.getConfigurationTool().getContainer()).setShapes(new ObjectArray(
                new ObjectElement(
                        new ObjectField("type", "rectangle"),
                        new ObjectField("name", "root"),
                        new ObjectField("description", "borderlayout"/*"flowlayout"*/),
                        new ObjectField("x", 1),
                        new ObjectField("y", 1),
                        new ObjectField("width", 400),
                        new ObjectField("height", 300),
                        new ObjectField("color", 1),
                        new ObjectField("strokeWidth", 1)
                ),
                new ObjectElement(
                        new ObjectField("type", "text"),
                        new ObjectField("name", "text1"),
                        new ObjectField("parentName", "root"),
                        new ObjectField("description", "label\nconstraints=\"NORTH\""),
                        new ObjectField("x", 10),
                        new ObjectField("y", 10),
                        new ObjectField("width", 280),
                        new ObjectField("height", 20),
                        new ObjectField("color", 167716),
                        new ObjectField("strokeWidth", 1),
                        new ObjectField("filled", true),
                        new ObjectField("text", "hello world"),
                        new ObjectField("fontSize", 10)
                ),
                new ObjectElement(
                        new ObjectField("type", "rectangle"),
                        new ObjectField("name", "input1"),
                        new ObjectField("parentName", "root"),
                        new ObjectField("description", "textarea\ncol=\"10\" constraints=\"SOUTH\""),
                        new ObjectField("x", 10),
                        new ObjectField("y", 40),
                        new ObjectField("width", 70),
                        new ObjectField("height", 50),
                        new ObjectField("color", 1),
                        new ObjectField("strokeWidth", 1)
                ),
                new ObjectElement(
                        new ObjectField("type", "rectangle"),
                        new ObjectField("name", "btn"),
                        new ObjectField("parentName", "root"),
                        new ObjectField("description", "button\nconstraints=\"EAST\""),
                        new ObjectField("x", 10),
                        new ObjectField("y", 100),
                        new ObjectField("width", 70),
                        new ObjectField("height", 20),
                        new ObjectField("color", 103546),
                        new ObjectField("text", "ok"),
                        new ObjectField("strokeWidth", 1),
                        new ObjectField("filled", false)
                ),
                new ObjectElement(
                        new ObjectField("type", "rectangle"),
                        new ObjectField("name", "panel1"),
                        new ObjectField("parentName", "root"),
                        new ObjectField("description", "panel\nconstraints=\"CENTER\""),
                        new ObjectField("x", 20),
                        new ObjectField("y", 150),
                        new ObjectField("width", 80),
                        new ObjectField("height", 50),
                        new ObjectField("color", 167716),
                        new ObjectField("strokeWidth", 2),
                        new ObjectField("filled", false)
                ),
                new ObjectElement(
                        new ObjectField("type", "rectangle"),
                        new ObjectField("name", "image1"),
                        new ObjectField("parentName", "panel1"),
                        new ObjectField("description", "label\nconstraints=\"CENTER\""),
                        new ObjectField("x", 1),
                        new ObjectField("y", 1),
                        new ObjectField("width", 80),
                        new ObjectField("height", 50),
                        new ObjectField("color", 1),
                        new ObjectField("strokeWidth", 1),
                        new ObjectField("filled", false),
                        new ObjectField("imageBytes", Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAACXBIWXMAAAsTAAALEwEAmpwYAAAAXklEQVR4nO2VsQ3AQAgDPR77j0AGcZqPFKXBvD6IRH8SHeYKFwB/xAAcAJgcH9kQnzjOmyTkWs5CNVcu4GOivX4ClS0IWVYyxdL7CFT6Crg7KBf42w/HJiWuvsxvcQKeF6eSI+Oa/gAAAABJRU5ErkJggg=="))
                )
        ));

        process.start();

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                null
                , null
                , null,
                List.of(
                        args -> {
                            System.out.println(args);
                            return new Action(
                                    List.of(
                                            new Message(new Value(1)),
                                            new Message(new Value("text1"))
                                    )
                                    , ActionType.EXECUTE
                            );
                        }
                )
                // , null
        );

        Thread thread = new Thread(() -> {
            process.execute(executionContextTool);
            executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
            executionContextTool.getOutput().clear();
        });
        thread.start();
        // process.execute(executionContextTool);

        Thread.sleep(1000);

        ExecutionContextToolImpl executionContextTool2 = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(new Value(1)),
                                                new Message(new Value("input1"))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
        process.execute(executionContextTool2);
        executionContextTool2.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool2.getOutput().clear();

        Thread.sleep(5000);

        ExecutionContextToolImpl executionContextTool3 = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(2)),
                                                new Message(MessageType.DATA, new Date(), new Value("input1")),
                                                new Message(MessageType.DATA, new Date(), new Value("test1"))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
        process.execute(executionContextTool3);
        executionContextTool3.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool3.getOutput().clear();

        Thread.sleep(5000);

        ExecutionContextToolImpl executionContextTool4 = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(MessageType.DATA, new Date(), new Value(0))
                                        ),
                                        ActionType.EXECUTE
                                ))), null, null);
        process.execute(executionContextTool4);
        executionContextTool4.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool4.getOutput().clear();

        // Thread.sleep(50000);
        thread.join();

        process.stop();
    }

}

