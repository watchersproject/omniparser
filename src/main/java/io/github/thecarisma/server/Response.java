package io.github.thecarisma.server;

import io.github.thecarisma.exceptions.ResponseHeaderException;
import io.github.thecarisma.laner.Attributes;
import io.github.thecarisma.laner.LanerPrintWriter;
import io.github.thecarisma.util.UserSystem;

import java.io.*;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Adewale Azeez <azeezadewale98@gmail.com>
 *
 *
 * https://expressjs.com/en/5x/api.html#res
 */
public class Response {

    protected LanerPrintWriter out;
    private Map<String, String[]> headers = new HashMap<>();
    private String rawResponseHead = "" ;
    private String reasonPhrase = "" ;
    private int statusCode = StatusCode.OK;
    private boolean headersNeedsParsing = true;
    public boolean headersSent = false;
    public boolean addDefaultHeaders = true;
    public final Server server ;

    public Response(Server server, LanerPrintWriter out) {
        this.server = server;
        this.out = out;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getReasonPhrase() {
        if (reasonPhrase.isEmpty()) {
            reasonPhrase = StatusCode.statusCodeValue(statusCode);
        }
        return reasonPhrase;
    }

    public void setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
    }

    public Map<String, String[]> getHeaders() {
        return headers;
    }

    public void appendHeader(String key, String... values) throws ResponseHeaderException {
        if (headersSent) {
            throw new ResponseHeaderException();
        }
        headers.put(key, values);
        headersNeedsParsing = true;
    }

    public String[] removeHeader(String key) {
        return headers.remove(key);
    }

    public String getHttpVersion() {
        return "HTTP/1.1";
    }

    public void write(String data) {
        if (!headersSent) {
            sendResponseHead();
        }
        out.write(data);
    }

    public void sendFile(File file) throws IOException, ResponseHeaderException {
        if (!headersSent) {
            BufferedWriter stream = new BufferedWriter(out.getWriter());
            BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
            long fileSize = file.length();
            appendHeader("Content-Length", "" + fileSize);
            appendHeader("Content-Type", getFileType(file));
            sendResponseHead();
            int i = -1;
            while((i = input.read()) != -1){
                stream.write(i);
            }
            stream.flush();
        }
    }

    public void close() {
        if (out.isOpen()) {
            out.close();
        }
    }

    private void sendResponseHead() {
        parseHeaders();
        headersSent = true;
        write(rawResponseHead);
    }

    private void parseHeaders() {
        if (headersNeedsParsing) {
            rawResponseHead = String.format("%s %d %s\r\n", getHttpVersion(), statusCode, getReasonPhrase());
            if (addDefaultHeaders) {
                try {
                    appendHeader("Host", server.getHost());
                    appendHeader("Date", new Date().toString());
                    appendHeader("User-Agent", String.format("%s/%s (%s; x%s)",
                            Attributes.NAME, Attributes.VERSION, UserSystem.OS, UserSystem.CPU_ARCH));
                } catch (ResponseHeaderException e) {
                    //impossible
                    e.printStackTrace();
                }
            }
            for (String key : headers.keySet()) {
                String[] values = headers.get(key);
                String header = key + ": ";
                for (int i = 0; i < values.length; ++i) {
                    header += values[i];
                    if (i != values.length - 1) {
                        header += ", ";
                    }
                }
                rawResponseHead += header + "\r\n";
            }
            rawResponseHead += "\r\n";
        }
    }

    private String getFileType(File file) throws IOException {
        String type = Files.probeContentType(file.toPath());
        return (type != null ?  type : "application/octet-stream") ;
    }

}
