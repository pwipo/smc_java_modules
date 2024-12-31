package ru.smcsystem.modules.module;

import org.junit.Test;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.smc.utils.ModuleUtils;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class WindowHtmlTest {

    @Test
    public void process() throws InterruptedException {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "configuration", new Value("<html>\n" +
                                        "   <head>\n" +
                                        " \n" +
                                        "   </head>\n" +
                                        "   <body>\n" +
                                        "     <p id=\"p_element\" style=\"margin-top: 0\">\n" +
                                        "       Hello world!\n" +
                                        "     </p>\n" +
                                        "     <div id=\"div_element\">\n" +
                                        "     </div>\n" +
                                        "   </body>\n" +
                                        " </html>\n"),
                                "width", new Value(300),
                                "height", new Value(100),
                                "title", new Value("MainForm"),
                                "ids", new Value("")

                        ),
                        null,
                        null
                ),
                new WindowHtml()
        );
        process.start();

        Thread thread = new Thread(() -> {
            ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null, null, null, null, "default", "SERVER");
            process.execute(executionContextTool);
            executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        });
        thread.start();

        Thread.sleep(1000);

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(List.of(new Action(List.of(new Message(new Value("p_element")))))),
                null, null, null, "default", "GET_ELEMENT");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(
                List.of(List.of(new Action(List.of(
                        new Message(new Value("div_element")),
                        // new Message(new Value("<div style=\"border-width: 1, border-color: red\"><strong>pwipo</strong><br/>Hello world!<br/><small>11.11.2000</small></div>")),
                        new Message(new Value("<table border=\"1\"><tr><td><table border=\"0\">" +
                                "<tr><td><strong>pwipo</strong></td></tr>" +
                                "<tr><td>Hello world!</td></tr>" +
                                "<tr><td><small>11.11.2000</small></td></tr>" +
                                "</table></td></tr></table>")),
                        new Message(new Value(-1)))))),
                null, null, null, "default", "ADD_CHILD_ELEMENT");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        executionContextTool = new ExecutionContextToolImpl(
                List.of(List.of(new Action(List.of(
                        new Message(new Value("div_element")),
                        // new Message(new Value("<div style=\"border-width: 1, border-color: red\"><strong>pwipo</strong><br/>Hello world!<br/><small>11.11.2000</small></div>")),
                        new Message(new Value("<table border=\"1\"><tr><td><table border=\"0\">" +
                                "<tr><td><strong>pwipo2</strong></td></tr>" +
                                "<tr><td>Hello world2!</td></tr>" +
                                "<tr><td><small>12.12.2000</small></td></tr>" +
                                "</table></td></tr></table>")),
                        new Message(new Value(-1)))))),
                null, null, null, "default", "ADD_CHILD_ELEMENT");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        thread.join();

        process.stop();
    }

    @Test
    public void processChat() throws InterruptedException {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "configuration", new Value("<html>\n" +
                                        "   <head>\n" +
                                        " \n" +
                                        "   </head>\n" +
                                        "   <body>\n" +
                                        "     <br/> \n" +
                                        "     <div id=\"div_element\">\n" +
                                        "     </div>\n" +
                                        "      <form>\n" +
                                        "         Username: <input name=\"user\" type=\"text\" /><br>\n" +
                                        "         Message: <textarea name=\"message\" cols=\"20\" rows=\"5\" /><br>\n" +
                                        "         <input name=\"submit\" type=\"submit\" value=\"submit\" />\n" +
                                        "         <input name=\"reset\" type=\"reset\" value=\"reset\" />\n" +
                                        "      </form>" +
                                        "   </body>\n" +
                                        " </html>\n"),
                                "width", new Value(300),
                                "height", new Value(300),
                                "title", new Value("MainForm"),
                                "ids", new Value("submit,reset")

                        ),
                        null,
                        null
                ),
                new WindowHtml()
        );
        process.start();

        Thread thread = new Thread(() -> {
            ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null, null, null,
                    List.of(lst -> {
                                ObjectElement objectElement = (ObjectElement) ((ObjectArray) lst.get(0)).get(0);
                                ExecutionContextToolImpl executionContextToolTmp = new ExecutionContextToolImpl(
                                        List.of(List.of(new Action(List.of(
                                                new Message(new Value("div_element")),
                                                // new Message(new Value("<div style=\"border-width: 1, border-color: red\"><strong>pwipo</strong><br/>Hello world!<br/><small>11.11.2000</small></div>")),
                                                new Message(new Value("<table border=\"1\"><tr><td><table border=\"0\">" +
                                                        "<tr><td><strong>" + objectElement.findField("user").map(ModuleUtils::getString).orElse("") + "</strong></td></tr>" +
                                                        "<tr><td>" + objectElement.findField("message").map(ModuleUtils::getString).orElse("") + "</td></tr>" +
                                                        "<tr><td><small>" + new Date().toString() + "</small></td></tr>" +
                                                        "</table></td></tr></table>")),
                                                new Message(new Value(-1)))))),
                                        null, null, null, "default", "ADD_CHILD_ELEMENT");
                                process.execute(executionContextToolTmp);

                                executionContextToolTmp = new ExecutionContextToolImpl(
                                        List.of(List.of(new Action(List.of(
                                                new Message(new Value("message")),
                                                new Message(new Value("")))))),
                                        null, null, null, "default", "SET_VALUE");
                                process.execute(executionContextToolTmp);

                                executionContextToolTmp = new ExecutionContextToolImpl(
                                        List.of(List.of(new Action(List.of(
                                                new Message(new Value(-1)))))),
                                        null, null, null, "default", "SET_POSITION");
                                process.execute(executionContextToolTmp);

                                return new Action(List.of());
                            },
                            lst -> {
                                ExecutionContextToolImpl executionContextToolTmp = new ExecutionContextToolImpl(
                                        List.of(List.of(new Action(List.of(
                                                new Message(new Value("div_element")),
                                                new Message(new Value("<div id=\"div_element\">\n</div>")))))),
                                        null, null, null, "default", "SET_ELEMENT");
                                process.execute(executionContextToolTmp);
                                return new Action(List.of());
                            }), "default", "SERVER");
            process.execute(executionContextTool);
            executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        });
        thread.start();

        Thread.sleep(1000);

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(List.of(new Action(List.of(new Message(new Value("p_element")))))),
                null, null, null, "default", "GET_ELEMENT");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        thread.join();

        process.stop();
    }

    @Test
    public void testForm() throws InterruptedException {
        Thread thread = new Thread(() -> {
            MainForm mainForm = new MainForm(
                    "MainForm",
                    "<html>\n" +
                            "   <head>\n" +
                            " \n" +
                            "   </head>\n" +
                            "   <body>\n" +
                            "     <p style=\"margin-top: 0\">\n" +
                            "       Hello world!\n" +
                            "     </p>\n" +
                            "   </body>\n" +
                            " </html>\n",
                    300, 300);
            mainForm.frame.setVisible(true);
            while (mainForm.frame.isVisible()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                }
            }
        });

        thread.start();
        thread.join();
    }

    @Test
    public void testSetValueObjectArray() throws InterruptedException {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "configuration", new Value("<html>\n" +
                                        "   <head>\n" +
                                        " \n" +
                                        "   </head>\n" +
                                        "   <body>\n" +
                                        "<select id=\"cars\">\n" +
                                        "  <option value=\"volvo\">Volvo</option>\n" +
                                        "  <option value=\"saab\">Saab</option>\n" +
                                        "  <option value=\"vw\">VW</option>\n" +
                                        "  <option value=\"audi\" selected>Audi</option>\n" +
                                        "</select>\n" +
                                        "   </body>\n" +
                                        " </html>\n"),
                                "width", new Value(300),
                                "height", new Value(100),
                                "title", new Value("MainForm"),
                                "ids", new Value("")

                        ),
                        null,
                        null
                ),
                new WindowHtml()
        );
        process.start();

        Thread thread = new Thread(() -> {
            ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null, null, null, null, "default", "server");
            process.execute(executionContextTool);
            executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        });
        thread.start();

        Thread.sleep(3000);

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(List.of(new Action(List.of(new Message(new Value("cars")))))),
                null, null, null, "default", "get_value");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        Thread.sleep(3000);

        executionContextTool = new ExecutionContextToolImpl(
                List.of(List.of(new Action(List.of(
                        new Message(new Value("cars")),
                        new Message(new Value(new ObjectArray(
                                new ObjectElement(new ObjectField("label", "volvo1"), new ObjectField("value", new ObjectElement(new ObjectField("result", 1))), new ObjectField("selected", true)),
                                new ObjectElement(new ObjectField("label", "saab1"), new ObjectField("value", 2)),
                                new ObjectElement(new ObjectField("label", "vw"), new ObjectField("value", 3))
                        )))
                )))),
                null, null, null, "default", "set_value");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        Thread.sleep(3000);

        executionContextTool = new ExecutionContextToolImpl(
                List.of(List.of(new Action(List.of(new Message(new Value("cars")))))),
                null, null, null, "default", "get_value");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        thread.join();

        process.stop();
    }

    @Test
    public void getAndSetSelect() throws InterruptedException {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "configuration", new Value("<html>\n" +
                                        "   <head>\n" +
                                        " \n" +
                                        "   </head>\n" +
                                        "   <body>\n" +
                                        "<select id=\"cars\">\n" +
                                        "  <option value=\"volvo\">Volvo</option>\n" +
                                        "  <option value=\"saab\">Saab</option>\n" +
                                        "  <option value=\"vw\">VW</option>\n" +
                                        "  <option value=\"audi\" selected>Audi</option>\n" +
                                        "</select>\n" +
                                        "   </body>\n" +
                                        " </html>\n"),
                                "width", new Value(300),
                                "height", new Value(100),
                                "title", new Value("MainForm"),
                                "ids", new Value("")

                        ),
                        null,
                        null
                ),
                new WindowHtml()
        );
        process.start();

        Thread thread = new Thread(() -> {
            ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(null, null, null, null, "default", "server");
            process.execute(executionContextTool);
            executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        });
        thread.start();

        Thread.sleep(3000);

        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(List.of(new Action(List.of(new Message(new Value("cars")))))),
                null, null, null, "default", "get_value");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        Thread.sleep(3000);

        executionContextTool = new ExecutionContextToolImpl(
                List.of(List.of(new Action(List.of(
                        new Message(new Value("cars")),
                        new Message(new Value("volvo1")),
                        new Message(new Value("saab1")),
                        new Message(new Value("vw1"))
                )))),
                null, null, null, "default", "set_select_options");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        Thread.sleep(3000);

        executionContextTool = new ExecutionContextToolImpl(
                List.of(List.of(new Action(List.of(new Message(new Value("cars")))))),
                null, null, null, "default", "get_value");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));

        thread.join();

        process.stop();
    }

}
