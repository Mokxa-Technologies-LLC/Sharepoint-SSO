package org.joget.mokxa.util;

import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.mokxa.model.ApiResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileServiceUtil {

    private final String client;
    private final Map config;
    private Object clientObject;

    private FormData formData;


    public FileServiceUtil(Map config) {
        this.config = config;
        LogUtil.info("FileServiceUtil",config.toString());
        this.client = getSafeString("client");
        initServiceClient();
    }

    public void setFormData(FormData formData) {
        this.formData = formData;
    }

    private void initServiceClient() {
        if ("SHAREPOINT".equals(client)) {
            clientObject = new SharePointUtil(
                    getSafeString("tenantId"),
                    getSafeString("clientId"),
                    getSafeString("clientSecret")
            );
        } else {
            throw new UnsupportedOperationException("Client not supported: " + client);
        }
    }

    public ApiResponse authenticate() {
        try {
            if ("SHAREPOINT".equals(client)) {
                return ((SharePointUtil) clientObject).authenticate();
            }else {
                throw new UnsupportedOperationException("Authentication not supported for client: " + client);
            }
        } catch (Exception e) {
            ApiResponse error = new ApiResponse();
            error.setResponseCode(500);
            error.setResponseBody("Authentication error: " + e.getMessage());
            return error;
        }
    }

    public ApiResponse createFolder(String path, String folderName) {
        try {
            if ("SHAREPOINT".equals(client)) {
                SharePointUtil sp = (SharePointUtil) clientObject;
                return sp.createFolder(getSafeString("siteId"), getSafeString("driveId"), path, folderName);
            } else {
                throw new UnsupportedOperationException("createFolder not supported for client: " + client);
            }
        } catch (Exception e) {
            ApiResponse error = new ApiResponse();
            error.setResponseCode(500);
            error.setResponseBody("Create folder error: " + e.getMessage());
            return error;
        }
    }

    public String uploadFile(String path, File file) {
        try {
            if ("SHAREPOINT".equals(client)) {
                SharePointUtil sp = (SharePointUtil) clientObject;
                return sp.createFile(getSafeString("siteId"), getSafeString("driveId"),path, file);
            }  else {
                throw new UnsupportedOperationException("uploadFile not supported for client: " + client);
            }
        } catch (Exception e) {
            ApiResponse error = new ApiResponse();
            error.setResponseCode(500);
            error.setResponseBody("Upload file error: " + e.getMessage());
            return null;
        }
    }

    public String downloadFile(String fileId) {
        try {
            if ("SHAREPOINT".equals(client)) {
                SharePointUtil sp = (SharePointUtil) clientObject;
                ApiResponse response=sp.getFile(getSafeString("siteId"), getSafeString("driveId"), fileId);
                JSONObject jsonObject= new JSONObject(response.getResponseBody());
                return jsonObject.getString("@microsoft.graph.downloadUrl");
            } else {
                throw new UnsupportedOperationException("getFile not supported for client: " + client);
            }
        } catch (Exception e) {

            return null;
        }
    }

    public String getEditLink(String fileId) {
        try {
            if ("SHAREPOINT".equals(client)) {
                SharePointUtil sp = (SharePointUtil) clientObject;
                return sp.getEditLink(getSafeString("siteId"), getSafeString("driveId"), fileId);

            } else {
                throw new UnsupportedOperationException("getFile not supported for client: " + client);
            }
        } catch (Exception e) {

            return null;
        }
    }

    public String viewFile(String fileId) {
        try {
            if ("SHAREPOINT".equals(client)) {
                SharePointUtil sp = (SharePointUtil) clientObject;
                ApiResponse response=sp.getFile(getSafeString("siteId"), getSafeString("driveId"), fileId);
                JSONObject jsonObject= new JSONObject(response.getResponseBody());
                return jsonObject.getString("webUrl");
            } else {
                throw new UnsupportedOperationException("getFile not supported for client: " + client);
            }
        } catch (Exception e) {

            return null;
        }
    }

    public ApiResponse deleteFile(String fileId) {
        try {
            if ("SHAREPOINT".equals(client)) {
                SharePointUtil sp = (SharePointUtil) clientObject;
                return sp.deleteFile(getSafeString("siteId"), getSafeString("driveId"), fileId);
            } else {
                throw new UnsupportedOperationException("deleteFile not supported for client: " + client);
            }
        } catch (Exception e) {
            ApiResponse error = new ApiResponse();
            error.setResponseCode(500);
            error.setResponseBody("Delete file error: " + e.getMessage());
            return error;
        }
    }

    private String getSafeString(String key) {
        Object value = config.get(key);
        if (value != null) {
            String str = value.toString().trim();
            if (!str.isEmpty()) {
                return str;
            }
        }
        return null;
    }

    private JSONObject generateMetaData(String key) {
        JSONObject result = new JSONObject();
        try {
            Object value = config.get(key);

            // Handle array from Joget config
            if (value instanceof Object[]) {
                for (Object obj : (Object[]) value) {
                    if (obj instanceof Map) {
                        Map map = (Map) obj;
                        String column = String.valueOf(map.get("column"));
                        String val = String.valueOf(map.get("value"));
                        val = resolvePlaceholders(val, formData);
                        column = toSharePointInternalName(column);
                        result.put(column, val);
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Error parsing JSON config for key: " + key);
        }
        return result;
    }

    private String resolvePlaceholders(String rawValue,FormData formData){
        if (rawValue == null || rawValue.trim().isEmpty()) return "";
        try {
            Matcher matcher = Pattern.compile("\\{([^{}]+)}").matcher(rawValue);
            StringBuffer resolved = new StringBuffer();

            while (matcher.find()) {
                String fieldId = matcher.group(1);
                String replacement = "";
                if (formData != null) {
                    String[] vals = formData.getRequestParameterValues(fieldId);
                    if (vals != null && vals.length > 0) {
                        replacement = vals[0];
                    }
                }
                matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(resolved);

            return resolved.toString();
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Error resolving {placeholders} in string: " + rawValue);
            return rawValue;
        }
    }

    private String toSharePointInternalName(String col) {
        if (col == null) return "";

        col = col.trim();

        // Replace all whitespace with SharePoint encoding
        col = col.replaceAll("\\s+", "");

        // Remove invalid characters for SP internal names
        col = col.replaceAll("[~#%&*{}\\\\/:<>?+|\"']", "");

        return col;
    }

    public void storeMetaToJoget(Map properties, String itemId,String jogetUser) {
        try {
            if ("SHAREPOINT".equals(client)) {
                SharePointUtil spUtil = (SharePointUtil) clientObject;
                String siteId = (String) properties.get("siteId");
                String driveId = (String) properties.get("driveId");
                String formDefId = (String) properties.get("formDefId");

                //form fields
                LogUtil.info(getClass().getName(), "Getting form fields " + itemId);
                String fileIdField = (String) properties.get("fileIdField");
                String versionField= (String) properties.get("versionField");
                String nameField= (String) properties.get("nameField");
                String descriptionField= (String) properties.get("descriptionField");
                String createdDateTimeField= (String) properties.get("createdDateTimeField");
                String lastModifiedDateTimeField= (String) properties.get("lastModifiedDateTimeField");
                String uploadedByField= (String) properties.get("uploadedByField");
                String sizeField= (String) properties.get("sizeField");
                String downloadUrlField= (String) properties.get("downloadUrlField");
                String documentTypeField= (String) properties.get("documentTypeField");
                String tagsValue= (String) properties.get("tagsValue");
                String tagsField= (String) properties.get("tagsField");
                String descriptionValue= (String) properties.get("descriptionValue");


                ApiResponse fileResp = spUtil.getFile(siteId, driveId, itemId);
                if (fileResp == null || fileResp.getResponseCode() != 200) {
                    LogUtil.warn(getClass().getName(), "Failed getting file metadata. itemId=" + itemId);
                    return;
                }



                JSONObject fileJson = new JSONObject(fileResp.getResponseBody());

                String id =UUID.randomUUID().toString();
                String name = fileJson.optString("name");
                String fileId = fileJson.optString("id");
                String size = fileJson.optJSONObject("size") != null ? fileJson.opt("size").toString() : fileJson.optString("size");
                String createdDate = fileJson.optString("createdDateTime");
                String modifiedDate = fileJson.optString("lastModifiedDateTime");


                ApiResponse versionResp = spUtil.listFileVersions(siteId, driveId, itemId);

                String latestVersionId = "";

                if (versionResp != null && versionResp.getResponseCode() == 200) {
                    JSONObject vJson = new JSONObject(versionResp.getResponseBody());

                    if (vJson.has("value")) {
                        JSONArray versions = vJson.getJSONArray("value");

                        String latestTime = "";
                        JSONObject latestObj = null;

                        for (int i = 0; i < versions.length(); i++) {
                            JSONObject v = versions.getJSONObject(i);
                            String modified = v.optString("lastModifiedDateTime", "");

                            if (latestObj == null || modified.compareTo(latestTime) > 0) {
                                latestObj = v;
                                latestTime = modified;
                            }
                        }

                        if (latestObj != null) {
                            latestVersionId = latestObj.optString("id");
                        }
                    }
                }


                AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
                AppDefinition appDef = AppUtil.getCurrentAppDefinition();
                String tableName = appService.getFormTableName(appDef, formDefId);

                LogUtil.info(getClass().getName(), "Setting Rows " + itemId);
                FormRow row = new FormRow();
                row.setId(id);

                safeSet(row, fileIdField, fileId);
                safeSet(row, nameField, name);
                safeSet(row, versionField, latestVersionId);
                safeSet(row, descriptionField, descriptionValue);
                safeSet(row, tagsField, tagsValue);
                safeSet(row, documentTypeField, getFileExtension(name));
                safeSet(row, createdDateTimeField, createdDate);
                safeSet(row, lastModifiedDateTimeField, modifiedDate);
                safeSet(row, uploadedByField, jogetUser);
                safeSet(row, sizeField, size);
                safeSet(row, downloadUrlField, fileJson.optString("@microsoft.graph.downloadUrl"));
                FormRowSet rowSet = new FormRowSet();
                rowSet.add(row);

                appService.storeFormData(formDefId, tableName,rowSet,id);

                LogUtil.info(getClass().getName(), "Metadata stored in Joget successfully for itemId: " + itemId);
            }
        } catch (Exception e) {
            LogUtil.warn(getClass().getName(), "Metadata stored in Joget failed for itemId: " + itemId);
            LogUtil.warn(getClass().getName(), "Error " + e);
        }
    }



    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1).toUpperCase();
    }

    private void safeSet(FormRow row, String field, String value) {
        if (field != null && !field.isEmpty()) {
            row.setProperty(field, value != null ? value : "");
        }
    }
}