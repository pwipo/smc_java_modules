package ru.smcsystem.modules.flowControllerCron;

import org.apache.commons.lang3.StringUtils;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;

import java.util.*;

public class Cron implements Module {

    // private Date lastDate;

    private class Time {
        public List<Integer> lstSeconds;
        public List<Integer> lstMinute;
        public List<Integer> lstHour;
        public List<Integer> lstDay;
        public List<Integer> lstDayOfWeek;

        public Time(List<Integer> lstSeconds, List<Integer> lstMinute, List<Integer> lstHour, List<Integer> lstDay, List<Integer> lstDayOfWeek) {
            this.lstSeconds = lstSeconds;
            this.lstMinute = lstMinute;
            this.lstHour = lstHour;
            this.lstDay = lstDay;
            this.lstDayOfWeek = lstDayOfWeek;
        }
    }

    private List<Time> times;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        String time = (String) configurationTool.getSetting("time").orElseThrow(() -> new ModuleException("time setting")).getValue();

        times=new LinkedList<>();
        String[] arrTimes = StringUtils.split(time, "\n");
        for (String strTime : arrTimes) {
            strTime = strTime.trim();
            if(strTime.isBlank())
                continue;
            String[] arr = StringUtils.split(strTime);
            if (arr.length < 4)
                throw new ModuleException("wrong format. need: * * * *");

            times.add(
                    new Time(
                            parse(arr[0])
                            , parse(arr[1])
                            , parse(arr[2])
                            , parse(arr[3])
                            , arr.length > 4 ? parse(arr[4]) : Collections.EMPTY_LIST
                    ));
        }
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        Date date = new Date();

        /*
        // check if we stay in same second
        if (lastDate != null && (lastDate.getTime() + 1000) > date.getTime())
            return;
        */

        Calendar c = Calendar.getInstance();
        c.setTime(date);

        for(Time time: times) {
            if (compare(time.lstDayOfWeek, c.get(Calendar.DAY_OF_WEEK)) && compare(time.lstDay, date.getDay()) && compare(time.lstHour, date.getHours()) && compare(time.lstMinute, date.getMinutes()) && compare(time.lstSeconds, date.getSeconds())) {
            /*
            for (int i = 0; i < executionContextTool.getFlowControlTool().countManagedExecutionContexts(); i++)
                executionContextTool.getFlowControlTool().executeNow(CommandType.EXECUTE, i, null);
            */
                executionContextTool.addMessage(1);
                break;
            }
        }

        // lastDate = date;
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        times = null;
    }

    private List<Integer> parse(String str) {

        List<Integer> lstResult = new ArrayList<>();

        if (str == null || str.isEmpty() || str.equals("*"))
            return lstResult;

        str = str.trim();


        String[] arrStr = new String[]{str};
        if (str.contains(";")) {
            arrStr = str.split(";");
        }

        for (String mainStr : arrStr) {
            if (mainStr.contains("-")) {
                int iStep = 1;
                String strTmp = mainStr;
                if (strTmp.contains("/")) {
                    String[] tmpArrStr = strTmp.split("/");
                    try {
                        iStep = Integer.parseInt(tmpArrStr[1]);
                    } catch (NumberFormatException e) {
                    }
                    strTmp = tmpArrStr[0];
                }

                String[] tmpArrStr = strTmp.split("-");

                try {
                    int tmpIStart = Integer.parseInt(tmpArrStr[0]);
                    int tmpIStop = Integer.parseInt(tmpArrStr[1]);
                    for (int i = tmpIStart; i <= tmpIStop; i = i + iStep)
                        lstResult.add(i);
                } catch (NumberFormatException e) {
                }
            } else {
                try {
                    lstResult.add(Integer.parseInt(mainStr));
                } catch (NumberFormatException e) {
                }
            }
        }

        return lstResult;
    }

    private boolean compare(List<Integer> lstVar, int iVar) {
        boolean result = false;

        if (lstVar.size() > 0) {
            for (int i : lstVar) {
                if (iVar == i) {
                    result = true;
                    break;
                }
            }
        } else
            result = true;

        return result;
    }

}
