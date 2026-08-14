package org.joget.mokxa;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringEscapeUtils;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.lib.FileUpload;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.FileManager;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.commons.util.StringUtil;
import org.joget.mokxa.model.ApiResponse;
import org.joget.mokxa.util.FileServiceUtil;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUploadElement extends FileUpload {
    private final static String MESSAGE_PATH = "messages/FileUploadElement";

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("org.joget.mokxa.fileUploadElement.pluginLabel", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getVersion() {
        return Activator.VERSION;
    }

    @Override
    public String getDescription() {
        return AppPluginUtil.getMessage("org.joget.mokxa.fileUploadElement.pluginDesc", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getFormBuilderCategory() {
        return "Mokxa Plugins";
    }

    @Override
    public String getLabel() {
        return AppPluginUtil.getMessage("org.joget.mokxa.fileUploadElement.pluginLabel", getClassName(), MESSAGE_PATH);
    }

    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/FileUploadElement.json", null, true, MESSAGE_PATH);
    }


    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        String template = "sharepointFileUpload.ftl";



        FileServiceUtil fileServiceUtil=null;
        try{
            fileServiceUtil = new FileServiceUtil(getProperties());
        }catch (Exception ex){
            LogUtil.error(getClassName(), ex, "Client Error");
            dataModel.put("error", "Client error: " + ex.getMessage());
        }

        if(fileServiceUtil==null){
            dataModel.put("error", "Configuration error" );
            return FormUtil.generateElementHtml(this, formData, template, dataModel);
        }


        String siteId = getPropertyString("siteId");
        String driveId = getPropertyString("driveId");

        Map<String, String> tempFilePaths = new LinkedHashMap<>();
        List<Map<String, String>> items = new ArrayList<>(); // [{filename, itemId, rawValue}]

        String appId = "";
        String appVersion = "";
        try {
            AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            if (appDef != null) {
                appId = appDef.getId();
                appVersion = appDef.getVersion().toString();
            }
        } catch (Exception ex) {
            LogUtil.warn(getClassName(), "Unable to determine app/form context: " + ex.getMessage());
        }

//        LogUtil.info(getClass().getName(), "App Id: " + appId + " App Version: " + appVersion);



        String uniqueSuffix = FormUtil.getElementParameterName(this)
                + "_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        dataModel.put("uniqueSuffix", uniqueSuffix);

        // 1) Get values: prefer stored DB value, fallback to temp request param
        String[] values = new String[0];
        try {
            String storedValue = formData.getStoreBinderDataProperty(this);
            if (storedValue != null && !storedValue.trim().isEmpty()) {
                values = storedValue.split(";");
            } else {
                String id = FormUtil.getElementParameterName(this);
                String[] tempExisting = formData.getRequestParameterValues(id + "_path");
                if (tempExisting != null && tempExisting.length > 0) {
                    values = tempExisting;
                }
            }
        } catch (Exception ex) {
            LogUtil.warn(getClassName(), "Error reading stored/temp values: " + ex.getMessage());
        }

        // also include any newly-selected property values not yet in stored data (fallback original behaviour)
        if (values.length == 0) {
            String[] propVals = FormUtil.getElementPropertyValues(this, formData);
            if (propVals != null) values = propVals;
        }

        // 2) Split each value into filename/itemId, detect local temp files vs remote items
        for (String v : values) {
            if (v == null || v.trim().isEmpty()) continue;

            File file = FileManager.getFileByPath(v);
            if (file != null && file.exists()) {
                tempFilePaths.put(v, file.getName());
                continue;
            }

            Map<String, String> fileMap = parseFileName(v);
            String filename = fileMap.get("filename");
            String itemId = fileMap.get("fileId");
            if (filename == null) {
                // not in name|id format -- treat whole value as filename, no itemId
                filename = v;
                itemId = "";
            }

            Map<String, String> item = new LinkedHashMap<>();
            item.put("filename", filename);
            item.put("itemId", itemId);
            item.put("rawValue", v);
            items.add(item);
        }

        dataModel.put("tempFilePaths", tempFilePaths);
        dataModel.put("items", items);

        // 3) Build base (itemId-less) config for async render/sync calls -- encrypted once
        boolean syncEnabled = getPropertyString("enableSyncFiles").equals("true")
                && formData.getRequestParameter("id") != null
                && !formData.getRequestParameter("id").trim().isEmpty();

        try {
            JSONObject baseConfig = new JSONObject();
            baseConfig.put("siteId", siteId != null ? siteId : "");
            baseConfig.put("driveId", driveId != null ? driveId : "");
            baseConfig.put("clientId", getProperty("clientId"));
            baseConfig.put("clientSecret", getProperty("clientSecret"));
            baseConfig.put("tenantId", getProperty("tenantId"));
            baseConfig.put("client", getProperty("client"));
            baseConfig.put("appId", appId);
            baseConfig.put("appVersion", appVersion);


            String safeBaseConfig;
            try {
                safeBaseConfig = StringUtil.escapeString(SecurityUtil.encrypt(baseConfig.toString()), StringUtil.TYPE_URL, null);
            } catch (Exception ex) {
                safeBaseConfig = URLEncoder.encode(baseConfig.toString(), "UTF-8");
            }

            String serviceBase = "/web/json/app/" + appId + "/" + appVersion + "/plugin/" + this.getClassName() + "/service?";

            //Auth URL
            String checkAuthUrl = serviceBase + "action=checkAuth&config=" + safeBaseConfig;
            dataModel.put("checkAuthServiceUrl", checkAuthUrl);

            // render: takes baseConfig + itemIds (comma separated, plain) -> returns links per itemId
            String renderUrl = serviceBase + "action=render&config=" + safeBaseConfig;
            dataModel.put("renderServiceUrl", renderUrl);

            if (syncEnabled) {
                JSONObject syncConfig = new JSONObject(baseConfig.toString());
                String resolvedUploadPath = resolveSafeUploadPath(getPropertyString("sharePointUploadPath"), formData);
                syncConfig.put("uploadPath", resolvedUploadPath);
                syncConfig.put("formDefId", FormUtil.findRootForm(this).getPropertyString("id"));
                syncConfig.put("tableName", FormUtil.findRootForm(this).getPropertyString("tableName"));
                syncConfig.put("recordId", formData.getRequestParameter("id"));
                syncConfig.put("fieldId", getPropertyString(FormUtil.PROPERTY_ID));

//                LogUtil.info(getClassName(), "Sync config id : " + formData.getPrimaryKeyValue());

                String safeSyncConfig;
                try {
                    safeSyncConfig = StringUtil.escapeString(SecurityUtil.encrypt(syncConfig.toString()), StringUtil.TYPE_URL, null);
                } catch (Exception ex) {
                    safeSyncConfig = URLEncoder.encode(syncConfig.toString(), "UTF-8");
                }
                String syncUrl = serviceBase + "action=sync&config=" + safeSyncConfig;
                dataModel.put("syncServiceUrl", syncUrl);
            }

            dataModel.put("syncEnabled", syncEnabled);

        } catch (Exception ex) {
            LogUtil.warn(getClassName(), "Failed to build async service URLs: " + ex.getMessage());
            dataModel.put("syncEnabled", false);
        }

        // 4) Render the template (fast, no SharePoint network calls happened above)
        try {
            String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
            if (html == null) {
                LogUtil.warn(getClassName(), "Generated HTML is null; returning fallback error HTML.");
                html = "<div class=\"form-fileupload\">Error rendering file upload control.</div>";
            } else {
                boolean includeMeta = false;
                try {
                    includeMeta = Boolean.parseBoolean(String.valueOf(dataModel.get("includeMetaData")));
                } catch (Exception ignored) { }
                if (includeMeta) {
                    html = html.replace("<div class=\"form-fileupload\">", "<span class=\"form-floating-label\">SharePoint</span><div class=\"form-fileupload\">");
                }
            }
            return html;
        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "Error generating element HTML.");
            return "<div class=\"form-fileupload\">Error rendering SharePoint upload element: " + StringEscapeUtils.escapeHtml(ex.getMessage()) + "</div>";
        }
    }

    @Override
    public FormData formatDataForValidation(FormData formData) {
        try {
            String filePathPostfix = "_path";
            String id = FormUtil.getElementParameterName(this);
            if (id == null) return formData;

            String[] tempFilenames = formData.getRequestParameterValues(id);
            String[] tempExisting = formData.getRequestParameterValues(id + filePathPostfix);
            String[] fileWithIds = FormUtil.getElementPropertyValues(this, formData);

            if (tempFilenames == null) tempFilenames = new String[0];
            if (tempExisting == null) tempExisting = new String[0];
            if (fileWithIds == null) fileWithIds = new String[0];

            if (tempExisting.length > 0 && fileWithIds.length > 0) {
                for (int i = 0; i < tempExisting.length; i++) {
                    for (String fw : fileWithIds) {
                        String[] parts = fw.split("\\|");
                        if (parts.length == 2) {
                            String filename = parts[0];
                            String itemId = parts[1];
                            if (tempExisting[i].equals(filename)) {
                                tempExisting[i] = filename + "|" + itemId;
                                break;
                            }
                        }
                    }
                }
            }

            List<String> filenames = new ArrayList<>();
            filenames.addAll(Arrays.asList(tempFilenames));
            filenames.addAll(Arrays.asList(tempExisting));

            if (filenames.isEmpty()) {
                formData.addRequestParameterValues(id, new String[]{""});
            } else if (!Boolean.parseBoolean(getPropertyString("multiple"))) {
                formData.addRequestParameterValues(id, new String[]{filenames.get(0)});
            } else {
                formData.addRequestParameterValues(id, filenames.toArray(new String[0]));
            }

        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Exception during validation");
            formData.addFormError(FormUtil.getElementParameterName(this), "System Error: See system logs");
        }
        return formData;
    }

    @Override
    public FormRowSet formatData(FormData formData) {
        Form form = FormUtil.findRootForm(this);
        String id = getPropertyString(FormUtil.PROPERTY_ID);
        FormRowSet rowSet = new FormRowSet();

        FileServiceUtil fileService = new FileServiceUtil(getProperties());
        ApiResponse authResponse = fileService.authenticate();

        if (authResponse == null || authResponse.getResponseCode() != 200) {
            formData.addFormError(id, "Authentication failed: " + (authResponse != null ? authResponse.getResponseBody() : "no response"));
            return null;
        }

        fileService.setFormData(formData);

        Set<String> existingSet = new LinkedHashSet<>();
        String originalValues = formData.getLoadBinderDataProperty(form, id);
        if (originalValues != null && !originalValues.trim().isEmpty()) {
            for (String s : originalValues.split(";")) {
                if (s != null && !s.trim().isEmpty()) {
                    existingSet.add(s.trim());
                }
            }
        }

        String[] values = FormUtil.getElementPropertyValues(this, formData);
        if (values == null) {
            values = new String[0];
        }

        Set<String> keptExisting = new LinkedHashSet<>();
        List<File> filesToUpload = new ArrayList<>();
        for (String v : values) {
            if (v == null || v.trim().isEmpty()) continue;
            File f = FileManager.getFileByPath(v);
            if (f != null) {
                filesToUpload.add(f);
            } else {
                keptExisting.add(v.trim());
            }
        }

        String uploadPath = resolveSafeUploadPath(getPropertyString("sharePointUploadPath"), formData);
        String sameFileMode = getPropertyString("sameFileUpload");

        Set<String> deletedFiles = new LinkedHashSet<>(existingSet);
        deletedFiles.removeAll(keptExisting);

        for (String removed : deletedFiles) {
            if (removed == null || removed.trim().isEmpty()) continue;
            Map<String, String> fileMap = parseFileName(removed);
            String filename = fileMap.get("filename");
            String itemId = fileMap.get("fileId");
            ApiResponse delResp = fileService.deleteFile(itemId);
            if (delResp != null && (delResp.getResponseCode() == 200 || delResp.getResponseCode() == 204)) {
                // deleted ok
            } else {
                LogUtil.warn(getClassName(), "Failed to delete removed file: " + filename + " → " + (delResp != null ? delResp.getResponseBody() : "no response"));
            }
        }

        Set<String> uploadedNames = new LinkedHashSet<>();
        for (File file : filesToUpload) {
            String fileName = file.getName();
            String fullPath = uploadPath.endsWith("/") ? uploadPath + fileName : uploadPath + "/" + fileName;

            boolean existsRemotely = existingSet.stream().anyMatch(s -> s.startsWith(fileName + "|") || s.equals(fileName))
                    || keptExisting.stream().anyMatch(s -> s.startsWith(fileName + "|") || s.equals(fileName));

            if (existsRemotely) {
                if ("replace".equalsIgnoreCase(sameFileMode)) {
                    String matched = existingSet.stream()
                            .filter(s -> s.startsWith(fileName + "|") || s.equals(fileName))
                            .findFirst()
                            .orElse(null);

                    String itemId = null;
                    if (matched != null && matched.contains("|")) {
                        itemId = matched.split("\\|")[1];
                    }

                    ApiResponse dresp;
                    if (itemId != null) {
                        dresp = fileService.deleteFile(itemId);
                    } else {
                        dresp = fileService.deleteFile(fullPath);
                    }
                    String newItemId = fileService.uploadFile(uploadPath, file);
                    if (newItemId != null) {
                        existingSet.removeIf(f -> f.startsWith(fileName + "|") || f.equals(fileName));
                        keptExisting.removeIf(f -> f.startsWith(fileName + "|") || f.equals(fileName));
                        uploadedNames.add(fileName + "|" + newItemId);
                        fileService.storeMetaToJoget(getProperties(), newItemId, AppUtil.processHashVariable("#currentUser.username#", null, null, null));
                    } else {
                        formData.addFormError(id, "Failed to upload (replace) " + fileName);
                    }
                } else if ("version".equalsIgnoreCase(sameFileMode)) {
                    String newItemId = fileService.uploadFile(uploadPath, file);
                    if (newItemId != null) {
                        keptExisting.removeIf(f -> f.startsWith(fileName + "|") || f.equals(fileName));
                        uploadedNames.add(fileName + "|" + newItemId);
                        fileService.storeMetaToJoget(getProperties(), newItemId, AppUtil.processHashVariable("#currentUser.username#", null, null, null));
                    } else {
                        formData.addFormError(id, "Failed to update/upload " + fileName);
                    }
                }
            } else {
                String newItemId = fileService.uploadFile(uploadPath, file);
                if (newItemId != null) {
                    uploadedNames.add(fileName + "|" + newItemId);
                    fileService.storeMetaToJoget(getProperties(), newItemId, AppUtil.processHashVariable("#currentUser.username#", null, null, null));
                } else {
                    formData.addFormError(id, "Failed to upload " + fileName);
                }
            }
        }

        LinkedHashSet<String> finalFiles = new LinkedHashSet<>();
        finalFiles.addAll(keptExisting);
        finalFiles.addAll(uploadedNames);

        Map<String, String> dedup = new LinkedHashMap<>();
        for (String f : finalFiles) {
            String base = f.contains("|") ? f.split("\\|")[0] : f;
            dedup.put(base, f);
        }
        finalFiles.clear();
        finalFiles.addAll(dedup.values());

        FormRow result = new FormRow();
        String delimitedValue = FormUtil.generateElementPropertyValues(finalFiles.toArray(new String[0]));
        result.setProperty(id, (delimitedValue != null) ? delimitedValue : "");
        rowSet.add(result);

        formData.addRequestParameterValues(id + "_path", finalFiles.toArray(new String[0]));

        return rowSet;
    }

    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        String mode = request.getParameter("mode");

        if ("download".equals(action)) {

            String params = SecurityUtil.decrypt(request.getParameter("params"));
            JSONObject jsonParams = new JSONObject(params);

            Map config = new HashMap();
            String client = jsonParams.getString("client");
            config.put("client", client);

            String filePath = "";

            if (client.equalsIgnoreCase("SHAREPOINT")) {
                config.put("siteId", jsonParams.getString("siteId"));
                config.put("driveId", jsonParams.getString("driveId"));
                config.put("clientId", jsonParams.getString("clientId"));
                config.put("clientSecret", jsonParams.getString("clientSecret"));
                config.put("tenantId", jsonParams.getString("tenantId"));
                filePath = jsonParams.getString("itemId");
            } else {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }

            FileServiceUtil fileServiceUtil = new FileServiceUtil(config);
            String downloadLink = fileServiceUtil.downloadFile(filePath);
            if (downloadLink == null) {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(buildFileNotFoundHtml("File not found in SharePoint. Please sync the folder and try again."));
                return;
            }
            response.sendRedirect(downloadLink);

        } else if ("edit".equals(action)) {

            String params = SecurityUtil.decrypt(request.getParameter("params"));
            JSONObject jsonParams = new JSONObject(params);

            Map config = new HashMap();
            String client = jsonParams.getString("client");
            config.put("client", client);

            String filePath = "";
            String itemId = "";

            if (client.equalsIgnoreCase("SHAREPOINT")) {
                config.put("siteId", jsonParams.getString("siteId"));
                config.put("driveId", jsonParams.getString("driveId"));
                itemId = jsonParams.getString("itemId");
                config.put("clientId", jsonParams.getString("clientId"));
                config.put("clientSecret", jsonParams.getString("clientSecret"));
                config.put("tenantId", jsonParams.getString("tenantId"));
                filePath = itemId;
            } else {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }

            FileServiceUtil fileServiceUtil = new FileServiceUtil(config);
            String downloadLink = fileServiceUtil.getEditLink(filePath);

            if (downloadLink == null) {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(buildFileNotFoundHtml("File not found in SharePoint. Please sync the folder and try again."));
                return;
            }

            if ("teams".equals(mode)) {
                downloadLink = String.format(
                        "https://teams.microsoft.com/l/file/%s?tenantId=%s&fileType=%s&objectUrl=%s",
                        filePath, jsonParams.getString("tenantId"), "docx", downloadLink
                );
            } else if ("native".equals(mode)) {
                String fileUrl = fileServiceUtil.getFilePath(itemId);
                String nativeUrl = "ms-word:ofe|u|" + fileUrl;

                String safeNativeUrl = nativeUrl
                        .replace("&", "&amp;")
                        .replace("\"", "&quot;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;");

                response.setContentType("text/html;charset=UTF-8");

                String html = "<!DOCTYPE html>"
                        + "<html lang='en'>"
                        + "<head>"
                        + "<meta charset='UTF-8'>"
                        + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                        + "<title>Open in Word</title>"
                        + "<style>"
                        + "*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }"
                        + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f5f5f5; display: flex; align-items: center; justify-content: center; min-height: 100vh; padding: 1rem; color: #1a1a1a; }"
                        + ".card { background: #fff; border: 1px solid #e0e0e0; border-radius: 12px; padding: 24px; max-width: 420px; width: 100%; }"
                        + ".header { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }"
                        + ".icon { width: 40px; height: 40px; background: #e8f0fe; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }"
                        + ".icon svg { width: 20px; height: 20px; color: #1a73e8; }"
                        + ".title { font-size: 15px; font-weight: 600; color: #1a1a1a; }"
                        + ".subtitle { font-size: 13px; color: #666; margin-top: 2px; }"
                        + ".divider { border: none; border-top: 1px solid #ececec; margin-bottom: 20px; }"
                        + ".actions { display: flex; justify-content: flex-end; gap: 8px; }"
                        + "button, .btn-open { font-size: 14px; font-family: inherit; padding: 8px 18px; border-radius: 8px; cursor: pointer; text-decoration: none; display: inline-flex; align-items: center; gap: 6px; transition: opacity .15s; }"
                        + ".btn-cancel { background: transparent; border: 1px solid #ccc; color: #444; }"
                        + ".btn-cancel:hover { background: #f5f5f5; }"
                        + ".btn-open { background: #1a73e8; border: 1px solid #1a73e8; color: #fff; }"
                        + ".btn-open:hover { opacity: .88; }"
                        + "</style>"
                        + "</head>"
                        + "<body>"
                        + "<div class='card'>"
                        + "  <div class='header'>"
                        + "    <div class='icon'>"
                        + "      <svg viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'><path d='M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z'/><polyline points='14 2 14 8 20 8'/><line x1='16' y1='13' x2='8' y2='13'/><line x1='16' y1='17' x2='8' y2='17'/><polyline points='10 9 9 9 8 9'/></svg>"
                        + "    </div>"
                        + "    <div>"
                        + "      <div class='title'>Open in Microsoft Word?</div>"
                        + "      <div class='subtitle'>This document will open in your desktop app.</div>"
                        + "    </div>"
                        + "  </div>"
                        + "  <hr class='divider'>"
                        + "  <div class='actions'>"
                        + "    <button class='btn-cancel' type='button' onclick='goBack()'>Cancel</button>"
                        + "    <a id='wordLink' class='btn-open' href=\"" + safeNativeUrl + "\">"
                        + "      <svg width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><path d='M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6'/><polyline points='15 3 21 3 21 9'/><line x1='10' y1='14' x2='21' y2='3'/></svg>"
                        + "      Open in Word"
                        + "    </a>"
                        + "  </div>"
                        + "</div>"
                        + "<script>"
                        + "function goBack() {"
                        + "  if (document.referrer && document.referrer !== window.location.href) {"
                        + "    window.location.href = document.referrer;"
                        + "  } else {"
                        + "    window.history.back();"
                        + "  }"
                        + "}"
                        + "document.getElementById('wordLink').addEventListener('click', function() {"
                        + "  setTimeout(goBack, 800);"
                        + "});"
                        + "</script>"
                        + "</body>"
                        + "</html>";

                response.getWriter().write(html);
                return;
            }

            response.sendRedirect(downloadLink);

        } else if ("sync".equals(action)) {
            handleSync(request, response);

        } else if ("render".equals(action)) {
            handleRender(request, response);
        } else if ("checkAuth".equals(action)) {
            handleCheckAuth(request, response);
        }else {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    private void handleSync(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject result = new JSONObject();
        try {
            String configStr = SecurityUtil.decrypt(request.getParameter("config"));
            JSONObject cfg = new JSONObject(configStr);

            Map config = new HashMap();
            config.put("client", cfg.getString("client"));
            config.put("siteId", cfg.getString("siteId"));
            config.put("driveId", cfg.getString("driveId"));
            config.put("clientId", cfg.getString("clientId"));
            config.put("clientSecret", cfg.getString("clientSecret"));
            config.put("tenantId", cfg.getString("tenantId"));

            String uploadPath = cfg.getString("uploadPath");
            String formDefId = cfg.getString("formDefId");
            String tableName = cfg.getString("tableName");
            String recordId = cfg.getString("recordId");
            String fieldId = cfg.getString("fieldId");

//            LogUtil.info(getClass().getName(), "Upload Form Def Id: " + formDefId);
//            LogUtil.info(getClass().getName(), "Upload Table Name: " + tableName);
//            LogUtil.info(getClass().getName(), "Upload Record Id: " + recordId);
//            LogUtil.info(getClass().getName(), "Upload Field Id: " + fieldId);
//            LogUtil.info(getClass().getName(), "Upload Path: " + uploadPath);

            FileServiceUtil fileServiceUtil = new FileServiceUtil(config);
            ApiResponse authResp = fileServiceUtil.authenticate();

            if (authResp == null || authResp.getResponseCode() != 200) {
                result.put("status", "error");
                result.put("message", "Authentication failed");
                writeJson(response, result);
                return;
            }

            FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
            FormRow existingRow = formDataDao.load(formDefId, tableName, recordId);

//            if (existingRow == null) {
//                result.put("status", "ok");
//                result.put("changed", false);
//                result.put("files", new JSONObject());
//                writeJson(response, result);
//                return;
//            }

            Map<String, String> dbMap = new LinkedHashMap<>();
            if (existingRow != null) {
                String storedValue = existingRow.getProperty(fieldId);
                if (storedValue != null && !storedValue.trim().isEmpty()) {
                    for (String v : storedValue.split(";")) {
                        if (v == null || v.trim().isEmpty()) continue;
                        String[] parts = v.split("\\|");
                        dbMap.put(parts[0], v);
                    }
                }
            }

//            LogUtil.info(getClass().getName(),"DB Map: "+dbMap);


            Map<String, String> spMap;
            try {
                spMap = buildSpMap(fileServiceUtil, uploadPath);
            } catch (Exception e) {
                result.put("status", "error");
                result.put("message", "Failed to sync files");
                writeJson(response, result);
                return;
            }

//            LogUtil.info(getClass().getName(),"Sp Map: "+spMap);

            boolean[] changed = new boolean[]{false};
            LinkedHashSet<String> finalSet = syncValues(dbMap, spMap, changed);


//            LogUtil.info(getClass().getName(), "Sync finished with changes " + finalSet);


            if (changed[0]) {
                // Use the existing row if available, otherwise create a new one
                FormRow rowToSave = (existingRow != null) ? existingRow : new FormRow();
                if (existingRow == null) {
                    rowToSave.setId(recordId);
                }

                if(FormUtil.generateElementPropertyValues(finalSet.toArray(new String[0]))==null){
                    rowToSave.setProperty(fieldId,"");
                }else{
                    rowToSave.setProperty(fieldId,FormUtil.generateElementPropertyValues(finalSet.toArray(new String[0])));
                }
                FormRowSet rs = new FormRowSet();
                rs.add(rowToSave);
                formDataDao.saveOrUpdate(formDefId, tableName, rs);

            }

            JSONObject filesObj = new JSONObject();
            for (String f : finalSet) {
                String[] parts = f.split("\\|");
                String filename = parts[0];
                String itemId = parts.length > 1 ? parts[1] : "";
                filesObj.put(filename, itemId);
            }

            result.put("status", "ok");
            result.put("changed", changed[0]);
            result.put("files", filesObj);

        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "Error during sync action");
            result.put("status", "error");
            result.put("message", "Error during sync files");
        }
        writeJson(response, result);
    }

    private void handleRender(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject result = new JSONObject();
        try {
            String configStr = SecurityUtil.decrypt(request.getParameter("config"));
            JSONObject baseConfig = new JSONObject(configStr);

            String itemIdsParam = request.getParameter("itemIds");
            String[] itemIds = (itemIdsParam != null && !itemIdsParam.trim().isEmpty())
                    ? itemIdsParam.split(",") : new String[0];

            JSONObject linksObj = new JSONObject();

            for (String itemId : itemIds) {
                itemId = itemId.trim();
                if (itemId.isEmpty()) continue;

                JSONObject linkParams = new JSONObject(baseConfig.toString());
                linkParams.put("itemId", itemId);

                String safeLinkParams;
                try {
                    safeLinkParams = StringUtil.escapeString(SecurityUtil.encrypt(linkParams.toString()), StringUtil.TYPE_URL, null);
                } catch (Exception ex) {
                    safeLinkParams = URLEncoder.encode(linkParams.toString(), "UTF-8");
                }

                String serviceBase = "/web/json/app/" + baseConfig.optString("appId", "") + "/" + baseConfig.optString("appVersion", "")
                        + "/plugin/" + this.getClassName() + "/service?";

                JSONObject entry = new JSONObject();
                entry.put("downloadUrl", serviceBase + "action=download&params=" + safeLinkParams);
                entry.put("editWebUrl", serviceBase + "action=edit&mode=web&params=" + safeLinkParams);
                entry.put("editTeamsUrl", serviceBase + "action=edit&mode=teams&params=" + safeLinkParams);
                entry.put("editNativeUrl", serviceBase + "action=edit&mode=native&params=" + safeLinkParams);

                linksObj.put(itemId, entry);
            }

            result.put("status", "ok");
            result.put("links", linksObj);

        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "Error during render action");
            result.put("status", "error");
            result.put("message", "Error rendering files.");
        }

        writeJson(response, result);
    }

    private void handleCheckAuth(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject result = new JSONObject();
        try {
            String configStr = SecurityUtil.decrypt(request.getParameter("config"));
            JSONObject cfg = new JSONObject(configStr);

            Map config = new HashMap();
            config.put("client", cfg.optString("client", "SHAREPOINT"));
            config.put("siteId", cfg.optString("siteId", ""));
            config.put("driveId", cfg.optString("driveId", ""));
            config.put("clientId", cfg.optString("clientId", ""));
            config.put("clientSecret", cfg.optString("clientSecret", ""));
            config.put("tenantId", cfg.optString("tenantId", ""));

            FileServiceUtil fileServiceUtil = new FileServiceUtil(config);
            ApiResponse authResp = fileServiceUtil.authenticate();

            if (authResp == null || authResp.getResponseCode() != 200) {
                result.put("status", "error");
                result.put("message", "SharePoint authentication failed. Upload and file actions are disabled.");
            } else {
                result.put("status", "ok");
            }
        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "Error during checkAuth");
            result.put("status", "error");
            result.put("message", "Configuration error. Please contact your administrator.");
        }
        writeJson(response, result);
    }

    private void writeJson(HttpServletResponse response, JSONObject obj) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write(obj.toString());
    }

    public String resolveSafeUploadPath(String rawPath, FormData formData) {
        if (rawPath == null || rawPath.trim().isEmpty()) return "/";
        try {
            String path = rawPath.trim().replace("\\", "/");

            Matcher m = Pattern.compile("\\{([^{}]+)}").matcher(path);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String fieldId = m.group(1);
                String value = "";
                if (formData != null) {
                    String[] vals = formData.getRequestParameterValues(fieldId);
                    if (vals != null && vals.length > 0) value = vals[0];
                }
                m.appendReplacement(sb, Matcher.quoteReplacement(value));
            }
            m.appendTail(sb);
            path = sb.toString();

            path = AppUtil.processHashVariable(path, null, null, null);

            path = path.replaceAll("/+", "/");
            if (!path.startsWith("/")) path = "/" + path;
            if (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);

            return path;
        } catch (Exception e) {
            LogUtil.error("PathResolver", e, "Error resolving upload path");
            return rawPath;
        }
    }

    public Map<String, String> parseFileName(String input) {
        Map<String, String> resultMap = new HashMap<>();
        if (input == null) return resultMap;

        String[] parts = input.split("\\|");
        if (parts.length == 2) {
            resultMap.put("filename", parts[0].trim());
            resultMap.put("fileId", parts[1].trim());
        }
        return resultMap;
    }

    private Map<String, String> buildSpMap(FileServiceUtil fileServiceUtil, String uploadPath) throws Exception {
        Map<String, String> spMap = new LinkedHashMap<>();
        List<Map<String, String>> spFiles = fileServiceUtil.listFilesFromFolder(uploadPath);
        for (Map<String, String> f : spFiles) {
            String name = f.get("name");
            String id = f.get("id");
            if (name != null && id != null) {
                spMap.put(name, id);
            }
        }
        return spMap;
    }

    private LinkedHashSet<String> syncValues(Map<String, String> dbMap, Map<String, String> spMap, boolean[] changedFlag) {
        LinkedHashSet<String> finalSet = new LinkedHashSet<>();
        boolean changed = false;

        for (Map.Entry<String, String> e : spMap.entrySet()) {
            String filename = e.getKey();
            String itemId = e.getValue();
            String dbVal = dbMap.get(filename);
            String newVal = filename + "|" + itemId;

            if (dbVal == null || !dbVal.equals(newVal)) {
                changed = true;
            }
            finalSet.add(newVal);
        }

        for (String dbFilename : dbMap.keySet()) {
            if (!spMap.containsKey(dbFilename)) {
                changed = true;
            }
        }

        changedFlag[0] = changed;
        return finalSet;
    }

    private String buildFileNotFoundHtml(String errorMessage) {
        String safeMessage = escapeHtml(
                errorMessage != null && !errorMessage.trim().isEmpty()
                        ? errorMessage
                        : "The requested file could not be found."
        );

        String html = "<!DOCTYPE html>"
                + "<html lang='en'>"
                + "<head>"
                + "<meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<title>File Not Found</title>"
                + "<style>"
                + "*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }"
                + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f5f5f5; display: flex; align-items: center; justify-content: center; min-height: 100vh; padding: 1rem; color: #1a1a1a; }"
                + ".card { background: #fff; border: 1px solid #e0e0e0; border-radius: 12px; padding: 24px; max-width: 440px; width: 100%; box-shadow: 0 4px 16px rgba(0,0,0,0.04); }"
                + ".header { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }"
                + ".icon { width: 42px; height: 42px; background: #fce8e6; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }"
                + ".icon svg { width: 22px; height: 22px; color: #d93025; }"
                + ".title { font-size: 15px; font-weight: 600; color: #1a1a1a; }"
                + ".subtitle { font-size: 13px; color: #666; margin-top: 2px; line-height: 1.4; }"
                + ".error-code { display: inline-block; font-size: 12px; font-weight: 600; color: #d93025; background: #fce8e6; border-radius: 999px; padding: 4px 10px; margin-bottom: 12px; }"
                + ".message { font-size: 13px; color: #444; line-height: 1.5; margin-bottom: 20px; }"
                + ".divider { border: none; border-top: 1px solid #ececec; margin-bottom: 20px; }"
                + ".actions { display: flex; justify-content: flex-end; gap: 8px; }"
                + "button, .btn-back { font-size: 14px; font-family: inherit; padding: 8px 18px; border-radius: 8px; cursor: pointer; text-decoration: none; display: inline-flex; align-items: center; gap: 6px; transition: opacity .15s, background .15s; }"
                + ".btn-cancel { background: transparent; border: 1px solid #ccc; color: #444; }"
                + ".btn-cancel:hover { background: #f5f5f5; }"
                + ".btn-back { background: #1a73e8; border: 1px solid #1a73e8; color: #fff; }"
                + ".btn-back:hover { opacity: .88; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='card'>"
                + "  <div class='header'>"
                + "    <div class='icon'>"
                + "      <svg viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'>"
                + "        <circle cx='12' cy='12' r='10'></circle>"
                + "        <line x1='12' y1='8' x2='12' y2='12'></line>"
                + "        <line x1='12' y1='16' x2='12.01' y2='16'></line>"
                + "      </svg>"
                + "    </div>"
                + "    <div>"
                + "      <div class='title'>File Not Found</div>"
                + "      <div class='subtitle'>The requested document is unavailable or may have been moved.</div>"
                + "    </div>"
                + "  </div>"
                + "  <div class='actions'>"
                + "    <button class='btn-back' type='button' onclick='goBack()'>"
                + "      <svg width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'>"
                + "        <polyline points='15 18 9 12 15 6'></polyline>"
                + "      </svg>"
                + "      Go Back"
                + "    </button>"
                + "  </div>"
                + "</div>"
                + "<script>"
                + "function goBack() {"
                + "  if (document.referrer && document.referrer !== window.location.href) {"
                + "    window.location.href = document.referrer;"
                + "  } else {"
                + "    window.history.back();"
                + "  }"
                + "}"
                + "</script>"
                + "</body>"
                + "</html>";

        return html;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}