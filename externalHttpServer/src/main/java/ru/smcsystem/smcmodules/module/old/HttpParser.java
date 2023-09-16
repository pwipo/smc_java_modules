package ru.smcsystem.smcmodules.module.old;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpParser {
    private static final String[][] HttpReplies = {{"100", "Continue"},
            {"101", "Switching Protocols"},
            {"200", "OK"},
            {"201", "Created"},
            {"202", "Accepted"},
            {"203", "Non-Authoritative Information"},
            {"204", "No Content"},
            {"205", "Reset Content"},
            {"206", "Partial Content"},
            {"300", "Multiple Choices"},
            {"301", "Moved Permanently"},
            {"302", "Found"},
            {"303", "See Other"},
            {"304", "Not Modified"},
            {"305", "Use Proxy"},
            {"306", "(Unused)"},
            {"307", "Temporary Redirect"},
            {"400", "Bad Request"},
            {"401", "Unauthorized"},
            {"402", "Payment Required"},
            {"403", "Forbidden"},
            {"404", "Not Found"},
            {"405", "Method Not Allowed"},
            {"406", "Not Acceptable"},
            {"407", "Proxy Authentication Required"},
            {"408", "Request Timeout"},
            {"409", "Conflict"},
            {"410", "Gone"},
            {"411", "Length Required"},
            {"412", "Precondition Failed"},
            {"413", "Request Entity Too Large"},
            {"414", "Request-URI Too Long"},
            {"415", "Unsupported Media Type"},
            {"416", "Requested Range Not Satisfiable"},
            {"417", "Expectation Failed"},
            {"500", "Internal Server Error"},
            {"501", "Not Implemented"},
            {"502", "Bad Gateway"},
            {"503", "Service Unavailable"},
            {"504", "Gateway Timeout"},
            {"505", "HTTP Version Not Supported"}};

    //private long longMaxContentLength=2000000;

    //private BufferedReader reader;
    private String method, url;
    private Map<String, String> headers;
    private Map<String, Object> params;
    private List<byte[]> lstMultipartBody;
    private List<Map<String, String>> lstMultipartHeaders;
    private int[] ver;
    private int replay;

    public HttpParser(/*InputStream is*/) {
        method = "";
        url = "";
        headers = new HashMap<>();
        params = new HashMap<>();
        lstMultipartBody = new ArrayList<>();
        lstMultipartHeaders = new ArrayList<>();
        ver = new int[2];
        //this.longMaxContentLength = longMaxContentLength;

        //try(BufferedReader reader = new BufferedReader(new InputStreamReader(is))){
		/*
		if(reader==null || !reader.ready())
			throw new IOException("wrong input");
			*/
        //}
    }

    public void process(InputStream is, long longMaxContentLength) throws IOException {
        method = "";
        url = "";
        headers.clear();
        params.clear();
        lstMultipartBody.clear();
        lstMultipartHeaders.clear();
        ver = new int[2];
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "ISO-8859-1"));
        replay = parseRequest(reader, longMaxContentLength);
    }

    private int parseRequest(BufferedReader reader, long longMaxContentLength) throws IOException {
        //System.out.println("start parseRequest");
        int result = 400;
		/*
		if(!reader.ready())
			return result;
			*/

        String initial = reader.readLine();
        if (initial == null || initial.isEmpty()) return 0;
        if (Character.isWhitespace(initial.charAt(0))) {
            //starting whitespace, return bad request
            return result;
        }

        StringTokenizer tokenizer = new StringTokenizer(initial);

        if (!tokenizer.hasMoreTokens())
            return result;
        method = tokenizer.nextToken();
        //System.out.println("get method " + method);

        if (!tokenizer.hasMoreTokens())
            return result;
        String strUrlTmp = tokenizer.nextToken();
        int idx = strUrlTmp.indexOf('?');
        if (idx < 0) {
            url = strUrlTmp;
        } else {
            url = strUrlTmp.substring(0, idx);//URLDecoder.decode(, "ISO-8859-1");
            parseQuery(strUrlTmp.substring(idx + 1), params);
        }
        //System.out.println("get url " + url);

        ver[0] = 0;
        ver[1] = 9;
        if (tokenizer.hasMoreTokens()) {
            String strHTTPVer = tokenizer.nextToken();
            if (strHTTPVer.indexOf("HTTP/") == 0 && strHTTPVer.indexOf('.') > 5) {
                String temp[] = strHTTPVer.substring(5).split("\\.");
                try {
                    ver[0] = Integer.parseInt(temp[0]);
                    ver[1] = Integer.parseInt(temp[1]);
                } catch (NumberFormatException nfe) {
                    return result;
                }
            }
        }
        //System.out.println("get version " + getVersion());

        headers = parseHeaders(reader);
        //System.out.println("get headers " + headers);

        if (ver[0] == 1 && ver[1] >= 1 && getHeader("Host") == null)
            return result;

        switch (method) {
            case "GET": {
                if (headers.isEmpty())
                    break;
                result = 200;
                break;
            }
            case "POST": {
                if (headers.isEmpty())
                    break;
                String strContentType = getHeader("Content-Type");
                String strContentLength = getHeader("Content-Length");
                if (strContentType == null || strContentLength == null) {
                    //no content in post
                    result = 411;
                    break;
                }
                Long longContentLength = null;
                try {
                    longContentLength = Long.valueOf(strContentLength);
                } catch (NumberFormatException e) {
                    result = 411;
                    break;
                }

                if (longContentLength > longMaxContentLength) {
                    result = 413;
                    break;
                }

                char arrCharMain[] = new char[longContentLength.intValue()];
                int intNumCharsInMainArr = reader.read(arrCharMain);
                if (intNumCharsInMainArr == -1)
                    return result;

                result = parsePost(strContentType, new String(arrCharMain, 0, intNumCharsInMainArr).getBytes(StandardCharsets.UTF_8), params, lstMultipartHeaders, lstMultipartBody);

                break;
            }
            case "HEAD":
            case "OPTIONS":
            case "PUT":
            case "DELETE":
            case "TRACE":
            case "CONNECT":
                result = 501;
                break;
            default:
                result = 405;
        }

        //System.out.println("stop parseRequest");
        return result;
    }

    static public Map<String, String> parseHeaders(BufferedReader reader) throws IOException {
        //System.out.println("start parseHeaders");
        Map<String, String> result = new HashMap<String, String>();

        if (!reader.ready())
            return result;

        // that fscking rfc822 allows multiple lines, we don't care now
        String line = reader.readLine();
        while (line != null && !line.isEmpty()) {
            //System.out.println(line.length());
            int idx = line.indexOf(':');
            if (idx < 0) {
                break;
            } else {
                result.put(line.substring(0, idx).toLowerCase(), line.substring(idx + 1).trim());
            }
            line = reader.readLine();
        }
        //System.out.println("stop parseHeaders");
        return result;
    }

    public String getMethod() {
        return method;
    }

    public String getHeader(String key) {
        if (headers != null)
            return (String) headers.get(key.toLowerCase());
        else
            return null;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getRequestURL() {
        return url;
    }

    public String getParam(String key) {
        return (String) params.get(key);
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public String getVersion() {
        return ver[0] + "." + ver[1];
    }

    /*
        public int compareVersion(int major, int minor) {
            if (major < ver[0])
                return -1;
            else if (major > ver[0])
                return 1;
            else if (minor < ver[1])
                return -1;
            else if (minor > ver[1])
                return 1;
            else
                return 0;
        }
    */
    public String getStringReply(/*int codevalue*/) {
        String key, strRet;
        int i;

        strRet = null;
        key = "" + /*codevalue*/replay;
        for (i = 0; i < HttpReplies.length; i++) {
            if (HttpReplies[i][0].equals(key)) {
                strRet = /*codevalue*/replay + " " + HttpReplies[i][1];
                break;
            }
        }

        return strRet;
    }

    /*
    public static String getDateHeader() {
        SimpleDateFormat format;
        String ret;

        format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        ret = "Date: " + format.format(new Date()) + " GMT";

        return ret;
    }
    */
    public int getReplay() {
        return replay;
    }

    public List<byte[]> getLstMultipartBody() {
        return lstMultipartBody;
    }

    public List<Map<String, String>> getLstMultipartHeaders() {
        return lstMultipartHeaders;
    }

    /*
    public Map<String, String> parseParams(String strParam) {
        Map<String, String> result = new HashMap<String, String>();

        String prms[] = strParam.split("&");

        //get params
        //params = new HashMap<String,String>();
        for (int i = 0; i < prms.length; i++) {
            String temp[] = prms[i].split("=");
            if (temp == null || temp.length < 1)
                continue;
            try {
                String strKey = URLDecoder.decode(temp[0], "ISO-8859-1");
                String strValue = "";
                if (temp.length == 2) {
                    // we use ISO-8859-1 as temporary charset and then
                    // String.getBytes("ISO-8859-1") to get the data
                    strValue = URLDecoder.decode(temp[1], "ISO-8859-1");
        		// }else if(temp.length == 1 && prms[i].indexOf('=') == prms[i].length()-1) {
        		// 	// handle empty string separatedly
        		// 	params.put(URLDecoder.decode(temp[0], "ISO-8859-1"), "");
                }
                params.put(strKey, strValue);
            } catch (Exception e) {
            }
        }

        return result;
    }
    */

    static public int parsePost(
            String strContentType, byte[] bytes,
            Map<String, Object> params,
            List<Map<String, String>> lstMultipartHeaders,
            List<byte[]> lstMultipartBody
    ) throws IOException {
        int result = 500;

        String strTmp = new String(bytes);

        //System.out.println(strTmp);
        //ByteArrayInputStream bais = new ByteArrayInputStream(arrCharMain, 0, intNumCharsInMainArr);

        if (strContentType.startsWith("application/x-www-form-urlencoded")) {
            BufferedReader reader = new BufferedReader(new StringReader(strTmp));
            String strParamLine = null;
            while ((strParamLine = reader.readLine()) != null && !strParamLine.isEmpty()) {
                //StringTokenizer tmpTokenizer = new StringTokenizer(strParamLine, "&=");
                /*
                Map<String, String> tmpParams = parseParams(strParamLine);
                if (!tmpParams.isEmpty())
                    params.putAll(tmpParams);
                */
                parseQuery(strParamLine, params);
            }
        } else if (strContentType.startsWith("multipart/")) {

            //get boundary
            String arrStrTmp[] = strContentType.split("boundary=");
            if (arrStrTmp.length < 2) {
                result = 412;
                return result;
            }
			    	/*
					boundary = arrStrBoundary[1];
			    	arrStrBoundary = boundary.split("\\s");
			    	if(arrStrBoundary!=null)
			    		boundary = arrStrBoundary[0];
					*/
            String boundary = arrStrTmp[1].split("\\s")[0];
            if (boundary.startsWith("\"")) {
			    		/*
			    		arrStrBoundary = boundary.split("\"");
			    		if(arrStrBoundary.length>1){
			    			boundary=arrStrBoundary[1];
			    		}else{
			    			boundary = arrStrBoundary[0];
			    		}
			    		*/
                boundary = boundary.substring(1).split("\"")[0];
            }
            //System.out.println(boundary);

            //get all parts
            arrStrTmp = strTmp.split(boundary);
            List<String> lstParts = new ArrayList<String>();
            //System.out.println("count parts " + arrStrTmp.length);
            for (String str : arrStrTmp) {
                if (str == null || str.isEmpty())
                    continue;

                String strTmpHolder = str;

                //delete all - from start
                boolean bClean = false;
                while (!bClean) {
                    if (!strTmpHolder.isEmpty() && strTmpHolder.startsWith("-")) {
                        strTmpHolder = strTmpHolder.substring(1);
                    } else {
                        bClean = true;
                    }
                }

                //delete all - from end
                bClean = false;
                while (!bClean) {
                    if (!strTmpHolder.isEmpty() && strTmpHolder.endsWith("-")) {
                        strTmpHolder = strTmpHolder.substring(0, strTmpHolder.length() - 1);
                    } else {
                        bClean = true;
                    }
                }
                strTmpHolder = strTmpHolder.trim();
                if (!strTmpHolder.isEmpty())
                    lstParts.add(strTmpHolder);
            }
            //System.out.println("count parts " + lstParts.size());

            //reader = new BufferedReader(new StringReader(strPart));
            //String strCurrentLine = null;
            //Map<String, String> mapTmpHeaders=new HashMap<String, String>();
            //String strBody="";
            //int intEmptyStringCount=0;
            //main loop processing every part
            for (String strPart : lstParts) {
                //arrStrTmp = strPart.split("\\r\\n\\r\\n");
                //System.out.println(strPart);
                arrStrTmp = strPart.split("(?m)^$");
                if (arrStrTmp.length < 2)
                    continue;
                int i = arrStrTmp[0].length();
                //int i = strPart.indexOf("\\r\\n\\r\\n");
                if (i < 0)
                    continue;

                //get content
                String strBody = strPart.substring(i).trim();
                if (strBody == null || strBody.isEmpty())
                    continue;

                //System.out.println(strBody.length());
                //get headers
                Map<String, String> mapTmpHeaders = parseHeaders(new BufferedReader(new StringReader(strPart.substring(0, i).trim())));
                //reader.
                //}


		  			/*
		  			while((strCurrentLine = reader.readLine())!=null){
			  			//System.out.println(strCurrentLine);

						//if new boundary - save older data and start new - new part
					  	if (strCurrentLine.contains(boundary)){
				  			strBody = strBody.trim();
					  		if(!strBody.isEmpty()){
					  			//System.out.println(mapTmpHeaders);
					  			//System.out.println(strBody.length());
					  			*/

                String strContentDisposition = mapTmpHeaders.get("content-disposition");
					  			/*
					  			if(strContentDisposition==null)
					  				strContentDisposition = mapTmpHeaders.get("Content-Disposition");
					  			*/
                //String strContentTypeTmp = mapTmpHeaders.get("Content-Type");

                if (strContentDisposition != null) {
                    //if hase headers
                    //String arrStringTmp[]=null;
                    String strAttrName = null;
                    String strAttrValue = null;

                    arrStrTmp = strContentDisposition.split("name=");
                    if (arrStrTmp != null && arrStrTmp.length > 1)
                        strAttrName = arrStrTmp[1];

                    if (strContentDisposition.contains("filename=")) {
                        //is file
                        lstMultipartHeaders.add(mapTmpHeaders);
                        lstMultipartBody.add(bytes);//strBody.getBytes("ISO-8859-1"));
							  			/*
							  			//set filename as param
							  			arrStrTmp = strContentDisposition.split("filename=");
							  			if(arrStrTmp!=null && arrStrTmp.length>1)
							  				strAttrValue = arrStrTmp[1];
							  				*/
                    } else if (strAttrName != null) {
                        //is param
                        strAttrValue = strBody;//URLDecoder.decode(strBody, "ISO-8859-1");
                    } else {
                        //is text or file or else
                        lstMultipartHeaders.add(mapTmpHeaders);
                        lstMultipartBody.add(bytes);//strBody.getBytes("ISO-8859-1"));
                    }

                    //System.out.println(strAttrName + " " + strAttrValue);
                    if (strAttrName != null && strAttrValue != null) {
                        //set param
                        if (strAttrName.startsWith("\"")) {
				  							/*
				  							arrStringTmp = strAttrName.split("\"");
				  							if(arrStringTmp.length>1){
				  								strAttrName=arrStringTmp[1];
				  							}else{
				  								strAttrName=arrStringTmp[0];
				  							}
				  							*/
                            strAttrName = strAttrName.substring(1).split("\"")[0];
                        }
                        if (strAttrValue.startsWith("\"")) {
				  							/*
				  							arrStringTmp = strAttrValue.split("\"");
				  							if(arrStringTmp.length>1){
				  								strAttrValue=arrStringTmp[1];
				  							}else{
				  								strAttrValue=arrStringTmp[0];
				  							}
				  							*/
                            strAttrValue = strAttrValue.substring(1).split("\"")[0];
                        }
                        params.put(URLDecoder.decode(strAttrName, "ISO-8859-1"), URLDecoder.decode(strAttrValue, "ISO-8859-1"));
                    }
                } else {
                    //is plain text
                    lstMultipartHeaders.add(mapTmpHeaders);
                    lstMultipartBody.add(bytes);//strBody.getBytes("ISO-8859-1"));
                }
					  			/*
					  		}
							if(++intEmptyStringCount>2)
								break;
					  		*//*
							//intEmptyStringCount=0;
				  			strBody="";
					  		//mapTmpHeaders.clear();
					  		mapTmpHeaders = parseHeaders(reader);
				  			//System.out.println(mapTmpHeaders);
					  		continue;
					  	}
					  	*//*
						if(strCurrentLine.isEmpty()){
			  				*//*
							if(!reader.ready())
								break;
							if(++intEmptyStringCount>2)
								break;
								*//*
							++intEmptyStringCount;
							continue;
						}
			  			*//*
						//intEmptyStringCount=0;

					  	strBody = strBody + strCurrentLine + "\n";
					  	*/
            }
        } else {
            // result = 415;
            lstMultipartHeaders.add(new HashMap<>());
            lstMultipartBody.add(bytes);

            return result;
        }

        result = 200;
        return result;
    }

    static public void parseQuery(String query, Map<String, Object> parameters)
            throws UnsupportedEncodingException {

        if (query != null) {
            String pairs[] = query.split("[&]");

            for (String pair : pairs) {
                String param[] = pair.split("[=]");

                String key = null;
                String value = null;
                if (param.length > 0) {
                    key = URLDecoder.decode(param[0],
                            System.getProperty("file.encoding"));
                }

                if (param.length > 1) {
                    value = URLDecoder.decode(param[1],
                            System.getProperty("file.encoding"));
                }

                if (parameters.containsKey(key)) {
                    Object obj = parameters.get(key);
                    if (obj instanceof List<?>) {
                        List<String> values = (List<String>) obj;
                        values.add(value);
                    } else if (obj instanceof String) {
                        List<String> values = new ArrayList<>();
                        values.add((String) obj);
                        values.add(value);
                        parameters.put(key, values);
                    }
                } else {
                    parameters.put(key, value);
                }
            }
        }
    }

}
