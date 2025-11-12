package org.joget.mokxa;

//import jakarta.servlet.http.HttpServletRequest;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormatDefault;

import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.commons.util.StringUtil;
import org.joget.mokxa.model.ApiResponse;
import org.joget.mokxa.util.FileServiceUtil;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class FileUploadFormatter extends DataListColumnFormatDefault {
    private final static String MESSAGE_PATH = "messages/FileUploadFormatter";


    public String getName() {
        //return AppPluginUtil.getMessage("org.joget.mokxa.fileUploadFormatter.pluginLabel", getClassName(), MESSAGE_PATH);
        return "File Upload Formatter";
    }

    public String getVersion() {
        return Activator.VERSION;
    }

    public String getClassName() {
        return getClass().getName();
    }

    public String getLabel() {
        //return AppPluginUtil.getMessage("org.joget.mokxa.fileUploadFormatter.pluginLabel", getClassName(), MESSAGE_PATH);
        return "File Upload Formatter";
    }

    public String getDescription() {
        //support i18n
       // return AppPluginUtil.getMessage("org.joget.mokxa.fileUploadFormatter.pluginDesc", getClassName(), MESSAGE_PATH);
        return "Direct Link to Download Files";
    }

    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/FileUploadFormatter.json", null, true, MESSAGE_PATH);
    }

    @Override
    public String format(DataList dataList, DataListColumn dataListColumn, Object row, Object value) {
        if (value == null || value.toString().trim().isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String[] fileNames = value.toString().split(";");

        try {
            FileServiceUtil fileServiceUtil;
            try {
                fileServiceUtil = new FileServiceUtil(getProperties());
            } catch (Exception ex) {
                LogUtil.error(getClassName(), ex, "Failed to initialize FileServiceUtil.");
                return "";
            }

            // Validate SharePoint authentication
            try {
                ApiResponse authResponse = fileServiceUtil.authenticate();
                if (authResponse == null || authResponse.getResponseCode() != 200) {
                    LogUtil.warn(getClassName(), "SharePoint authentication failed or returned non-200.");
                } else {
                    LogUtil.info(getClassName(), "SharePoint authentication successful.");
                }
            } catch (Exception ex) {
                LogUtil.error(getClassName(), ex, "Unexpected error during SharePoint authentication.");
            }

            // Core parameters (constant for all files)
            JSONObject baseParams = new JSONObject();
            baseParams.put("client", getPropertyString("client"));

            if(getPropertyString("client").equalsIgnoreCase("SHAREPOINT")){
                baseParams.put("siteId", getPropertyString("siteId"));
                baseParams.put("driveId", getPropertyString("driveId"));
                baseParams.put("clientId", getPropertyString("clientId"));
                baseParams.put("clientSecret", getPropertyString("clientSecret"));
                baseParams.put("tenantId", getPropertyString("tenantId"));
            }


            AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            HttpServletRequest request = WorkflowUtil.getHttpServletRequest();

            String uniqueId = "fileList_" + System.currentTimeMillis() + "_" + Math.round(Math.random()*9999);

            // main container: flex row, wrap
            result.append("<div id='" + uniqueId + "' style='display:flex; flex-wrap:wrap; gap:6px; align-items:flex-start;'>");

            int total = fileNames.length;
            int index = 0;

            for (String fileName : fileNames) {

                if (fileName == null || fileName.trim().isEmpty()) continue;

                JSONObject params = new JSONObject(baseParams.toString());

                String[] verticalBarSplit = fileName.split("\\|");
                fileName = verticalBarSplit[0];
                String ItemId = verticalBarSplit[1];

                if(getPropertyString("client").equalsIgnoreCase("SHAREPOINT")) {
                    params.put("itemId", ItemId);
                }

                String encryptedParams;
                try {
                    encryptedParams = StringUtil.escapeString(SecurityUtil.encrypt(params.toString()), StringUtil.TYPE_URL, null);
                } catch (Exception ex) {
                    encryptedParams = URLEncoder.encode(params.toString(), StandardCharsets.UTF_8.name());
                }

                String downloadUrl = String.format(
                        "%s/web/json/app/%s/%s/plugin/org.joget.mokxa.FileUploadElement/service?action=download&params=%s",
                        request.getContextPath(),
                        appDef.getAppId(),
                        appDef.getVersion().toString(),
                        encryptedParams
                );

                String ext = getExtension(fileName);

                String cardHtml =
                        "<a href='" + downloadUrl + "' target='_blank' style='text-decoration:none; color:inherit;'>"
                                + "<div style='display:flex; align-items:center; border:1px solid #ddd; border-radius:8px; padding:6px 8px; width:200px; background:#fafafa;'>"
                                + "<div style='flex:0 0 34px; height:24px; border:1px solid #ccc; border-radius:6px; display:flex; align-items:center; justify-content:center; background:#ffffff; font-size:10px; font-weight:700; color:#444;'>"
                                + ext
                                + "</div>"
                                + "<div style='flex:1; padding-left:8px; font-size:12px; font-weight:600; color:#0078d4; word-break:break-word;'>"
                                + fileName
                                + "</div>"
                                + "</div>"
                                + "</a>";

                // Show only first card outside and put others in hidden extra area
                if (index == 0) {
                    result.append(cardHtml);

                    // open hidden container for other files
                    result.append("<div id='" + uniqueId + "_extra' style='display:none; flex-wrap:wrap; gap:6px; width:100%; margin-top:4px;'>");
                } else {
                    result.append(cardHtml);
                }

                index++;
            }

            result.append("</div>"); // close extra container

            // Add expand/collapse toggle
            if (total > 1) {
                int more = total - 1;

                result.append(
                        "<div id='" + uniqueId + "_toggle' "
                                + "style='cursor:pointer; color:#0078d4; font-size:12px; align-self:center;' "
                                + "onclick=\""
                                + "var extra=document.getElementById('" + uniqueId + "_extra');"
                                + "var toggle=document.getElementById('" + uniqueId + "_toggle');"
                                + "if(extra.style.display==='none'){extra.style.display='flex'; toggle.innerHTML='− collapse';}"
                                + "else{extra.style.display='none'; toggle.innerHTML='+" + " " + more + " more';}"
                                + "\">"
                                + "+ " + more + " more"
                                + "</div>"
                );
            }

            result.append("</div>"); // close main container

        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Error formatting SharePoint file links.");
        }

        // Clean up trailing semicolon
        String output = result.toString().trim();
        if (output.endsWith(";")) {
            output = output.substring(0, output.length() - 1);
        }

        return output;
    }

    private String getExtension(String name) {
        if (name == null) return "FILE";
        int i = name.lastIndexOf('.');
        if (i <= 0 || i == name.length()-1) return "FILE";
        return name.substring(i+1).toUpperCase();
    }
}
