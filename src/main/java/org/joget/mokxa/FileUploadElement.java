package org.joget.mokxa;

import org.apache.commons.lang.StringEscapeUtils;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
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


//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


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
        //String template = "fileUpload.ftl";
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
        String sharePointPathField = getPropertyString("sharePointUploadPathField");

        JSONObject jsonParams = new JSONObject();
        Map<String, String> tempFilePaths = new HashMap<>();
        Map<String, String> filePaths = new HashMap<>();
        Map<String, String> editPaths = new HashMap<>();

        String appId = "";
        String appVersion = "";

        try {
            // 1) Authenticate (best-effort). If fails, continue but set error in dataModel
            ApiResponse authResponse = fileServiceUtil.authenticate();
            if (authResponse == null || authResponse.getResponseCode() != 200) {
                dataModel.put("error", "Authentication Failed");
                LogUtil.warn(getClassName(), "SharePoint authentication failed or returned non-200.");
            } else {
                LogUtil.info(getClassName(), "SharePoint authentication successful.");
            }
        } catch (Exception ex) {
            LogUtil.error(getClassName(), ex, "Unexpected error during SharePoint authentication.");
            dataModel.put("error", "Authentication error: " + ex.getMessage());
        }

        // 2) Prepare values array defensively
        String[] values = FormUtil.getElementPropertyValues(this, formData);
        if (values == null) {
            values = new String[0];
        }


        // aaaa) Strip itemId for UI display, but keep full mapping internally
        List<String> uiValues = new ArrayList<>();
        Map<String, String> fullMap = new LinkedHashMap<>();
        for (String v : values) {
            if (v == null || v.trim().isEmpty()) continue;
            String filename = v;
            String itemId = "";
            if (v.contains("|")) {
                String[] parts = v.split("\\|");
                filename = parts[0];
                if (parts.length > 1) itemId = parts[1];
            }
            fullMap.put(filename, v);
            uiValues.add(filename);
        }
        values = uiValues.toArray(new String[0]);
        dataModel.put("fullValuesMap", fullMap);

        // If there is a stored (saved) value, prefer it
        try {
            String storedValue = formData.getStoreBinderDataProperty(this);
            if (storedValue != null && !storedValue.trim().isEmpty()) {
                values = storedValue.split(";");
            } else {
                // fallback to temp path request parameter (like original)
                String id = FormUtil.getElementParameterName(this);
                String[] tempExisting = formData.getRequestParameterValues(id + "_path");
                if (tempExisting != null && tempExisting.length > 0) {
                    values = tempExisting;
                }
            }
        } catch (Exception ex) {
            LogUtil.warn(getClassName(), "Error reading stored/temp values, continuing with existing values: " + ex.getMessage());
        }

        // 3) Get app context safely
        try {
            AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            if (appDef != null) {
                appId = appDef.getId();
                appVersion = appDef.getVersion().toString();
            }
        } catch (Exception ex) {
            LogUtil.warn(getClassName(), "Unable to determine app/form context: " + ex.getMessage());
        }

        // 4) Process each value (either temp file or stored SharePoint reference)
        if (values != null && values.length > 0) {
            for (String value : values) {
                try {
                    if (value == null || value.trim().isEmpty()) {
                        continue;
                    }
                    LogUtil.info("File",value);
                    String fullValue = fullMap.get(value);
                    Map<String,String> fileMap = parseFileName(fullValue);
                    //Map<String,String> fileMap=parseFileName(value);
                    String filename = fileMap.get("fileName");
                    String itemId = fileMap.get("fileId");

                    // check if actual temp file exists on disk
                    File file = FileManager.getFileByPath(value);
                    if (file != null && file.exists()) {
                        tempFilePaths.put(value, file.getName());
                        continue;
                    }

                    jsonParams = new JSONObject(); // reset per file
                    jsonParams.put("siteId", siteId != null ? siteId : "");
                    jsonParams.put("driveId", driveId != null ? driveId : "");
                    jsonParams.put("itemId", itemId);
                    jsonParams.put("clientId", getProperty("clientId"));
                    jsonParams.put("clientSecret", getProperty("clientSecret"));
                    jsonParams.put("tenantId", getProperty("tenantId"));
                    jsonParams.put("client", getProperty("client"));

                    LogUtil.info("Request Params",jsonParams.toString());

                    String safeParams = "";
                    try {
                        safeParams = StringUtil.escapeString(SecurityUtil.encrypt(jsonParams.toString()), StringUtil.TYPE_URL, null);
                    } catch (Exception ex) {
                        LogUtil.warn(getClassName(), "Failed to encrypt params for file " + filename + ": " + ex.getMessage());
                        safeParams = URLEncoder.encode(jsonParams.toString(), "UTF-8");
                    }

                    String filePath = "/web/json/app/" + appId + "/" + appVersion
                            + "/plugin/" + this.getClassName() + "/service?action=download&params=" + safeParams;
                    String editPath= "/web/json/app/" + appId + "/" + appVersion
                            + "/plugin/" + this.getClassName() + "/service?action=edit&params=" + safeParams;
                    LogUtil.info("Filepaths:",filePath);
                    filePaths.put(filePath, value);
                    editPaths.put(value,editPath);
                } catch (Exception ex) {
                    LogUtil.error(getClassName(), ex, "Error processing value: " + value);
                }
            }
        }

        // 5) Put maps into dataModel for Freemarker template
        try {
            dataModel.put("tempFilePaths", tempFilePaths);
            dataModel.put("filePaths", filePaths);
            dataModel.put("editLinks", editPaths);
        } catch (Exception ex) {
            LogUtil.warn(getClassName(), "Unable to set dataModel attributes: " + ex.getMessage());
        }

        // 6) Render template — ensure we never return null; return an HTML error fragment if rendering fails
        try {
            String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
            LogUtil.info("Html",html);
            if (html == null) {
                LogUtil.warn(getClassName(), "Generated HTML is null; returning fallback error HTML.");
                html = "<div class=\"form-fileupload\">Error rendering file upload control.</div>";
            } else {
                // optionally prepend floating label if metadata requested (null-safe)
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
            // provide a simple fallback HTML so caller doesn't get null
            return "<div class=\"form-fileupload\">Error rendering SharePoint upload element: " + StringEscapeUtils.escapeHtml(ex.getMessage()) + "</div>";
        }
    }


    @Override
    public FormData formatDataForValidation(FormData formData) {
        try {
            LogUtil.info(getClassName(), "Set validations started");
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

            LogUtil.info(getClassName(), "Set validations finished");
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

        // 1) Load previously saved filenames (semicolon separated)
        Set<String> existingSet = new LinkedHashSet<>();
        String originalValues = formData.getLoadBinderDataProperty(form, id);
        if (originalValues != null && !originalValues.trim().isEmpty()) {
            for (String s : originalValues.split(";")) {
                if (s != null && !s.trim().isEmpty()) {
                    existingSet.add(s.trim());
                }
            }
        }

        // 2) Get submitted values from form (can be temp file paths or existing filenames)
        String[] values = FormUtil.getElementPropertyValues(this, formData);
        if (values == null) {
            values = new String[0];
        }

        // 3) Identify kept existing filenames and files to upload
        Set<String> keptExisting = new LinkedHashSet<>();
        List<File> filesToUpload = new ArrayList<>();
        for (String v : values) {
            if (v == null || v.trim().isEmpty()) continue;
            File f = FileManager.getFileByPath(v);
            if (f != null) {
                // a temp file that needs uploading
                filesToUpload.add(f);
            } else {
                // a filename that user kept (or already stored)
                // If caller sends name|id format, take only filename part
                String nameOnly = v;
                if (v.contains("|")) {
                    String[] parts = v.split("\\|");
                    if (parts.length > 0) nameOnly = parts[0];
                }
                //keptExisting.add(nameOnly.trim());
                keptExisting.add(v.trim());
            }
        }

        String uploadPath = resolveSafeUploadPath(getPropertyString("sharePointUploadPath"),formData);
        String sameFileMode = getPropertyString("sameFileUpload"); // "replace" or "version"

        // 4) Compute deleted files (present in existingSet but not keptExisting)
        Set<String> deletedFiles = new LinkedHashSet<>(existingSet);
        deletedFiles.removeAll(keptExisting);
        // Note: if a new upload has same filename as deleted, we will handle accordingly below

        // 5) Delete removed files from SharePoint
        for (String removed : deletedFiles) {
            if (removed == null || removed.trim().isEmpty()) continue;
            //String fullPath = uploadPath.endsWith("/") ? uploadPath + removed : uploadPath + "/" + removed;
            Map<String,String> fileMap=parseFileName(removed);
            String filename = fileMap.get("fileName");
            String itemId = fileMap.get("fileId");
            ApiResponse delResp = fileService.deleteFile(itemId);
            if (delResp != null && (delResp.getResponseCode() == 200 || delResp.getResponseCode() == 204)) {
                LogUtil.info(getClassName(), "Deleted removed file from SharePoint: " + filename);
            } else {
                LogUtil.warn(getClassName(), "Failed to delete removed file: " + filename + " → " + (delResp != null ? delResp.getResponseBody() : "no response"));
            }
        }

        // 6) Upload new files (and handle replace/version if a same-name exists)
        Set<String> uploadedNames = new LinkedHashSet<>();
        for (File file : filesToUpload) {
            String fileName = file.getName();
            String fullPath = uploadPath.endsWith("/") ? uploadPath + fileName : uploadPath + "/" + fileName;

            //boolean existsRemotely = existingSet.contains(fileName) || keptExisting.contains(fileName);
            boolean existsRemotely = existingSet.stream().anyMatch(s -> s.startsWith(fileName + "|") || s.equals(fileName))
                    || keptExisting.stream().anyMatch(s -> s.startsWith(fileName + "|") || s.equals(fileName));

            if (existsRemotely) {
                if ("replace".equalsIgnoreCase(sameFileMode)) {
                    // delete remote first then upload
                    String matched = existingSet.stream()
                            .filter(s -> s.startsWith(fileName + "|") || s.equals(fileName))
                            .findFirst()
                            .orElse(null);

                    String itemId = null;
                    if (matched != null && matched.contains("|")) {
                        itemId = matched.split("\\|")[1];
                    }

                    // If we have ID → delete by ID
                    ApiResponse dresp;
                    if (itemId != null) {
                        dresp = fileService.deleteFile(itemId);
                    } else {
                        // fallback: delete by path
                        dresp = fileService.deleteFile(fullPath);
                    }
                    if (dresp != null && (dresp.getResponseCode() == 200 || dresp.getResponseCode() == 204)) {
                        LogUtil.info(getClassName(), "Deleted (replace mode) remote file before re-upload: " + fullPath);
                    }
                    String ItemId = fileService.uploadFile(uploadPath, file);
                    if (ItemId != null) {
                        //uploadedNames.add(fileName);
                        existingSet.removeIf(f -> f.startsWith(fileName + "|") || f.equals(fileName));
                        keptExisting.removeIf(f -> f.startsWith(fileName + "|") || f.equals(fileName));
                        uploadedNames.add(fileName + "|" + ItemId);
                        LogUtil.info(getClassName(), "Replaced file on SharePoint: " + fullPath);
                        fileService.storeMetaToJoget(getProperties(),itemId,AppUtil.processHashVariable("#currentUser.username#",null,null,null));
                    } else {
                        formData.addFormError(id, "Failed to upload (replace) " + fileName);
                    }
                } else if ("version".equalsIgnoreCase(sameFileMode)) {
                    String ItemId = fileService.uploadFile(uploadPath, file);
                    if (ItemId != null) {
                        //uploadedNames.add(fileName);
                        keptExisting.removeIf(f -> f.startsWith(fileName + "|") || f.equals(fileName));
                        uploadedNames.add(fileName + "|" + ItemId);
                        LogUtil.info(getClassName(), "Uploaded new Version file on SharePoint: " + fullPath);
                        fileService.storeMetaToJoget(getProperties(),ItemId,AppUtil.processHashVariable("#currentUser.username#",null,null,null));
                    } else {
                        formData.addFormError(id, "Failed to update/upload " + fileName);
                    }
                }
            } else {
                String ItemId = fileService.uploadFile(uploadPath, file);
                if (ItemId != null) {
                    //uploadedNames.add(fileName);
                    uploadedNames.add(fileName + "|" + ItemId);
                    LogUtil.info(getClassName(), "Uploaded new file on SharePoint: " + fullPath);
                    fileService.storeMetaToJoget(getProperties(),ItemId,AppUtil.processHashVariable("#currentUser.username#",null,null,null));
                } else {
                    formData.addFormError(id, "Failed to upload " + fileName);
                }
            }
        }

        // 7) Build final list to store back in Joget: keptExisting + uploadedNames (deduped, keep order)
        LinkedHashSet<String> finalFiles = new LinkedHashSet<>();
        finalFiles.addAll(keptExisting);   // files user kept
        finalFiles.addAll(uploadedNames);  // newly uploaded files (added at end)


        Map<String, String> dedup = new LinkedHashMap<>();
        for (String f : finalFiles) {
            String base = f.contains("|") ? f.split("\\|")[0] : f;
            dedup.put(base, f);
        }
        finalFiles.clear();
        finalFiles.addAll(dedup.values());


        // Save finalFiles back to Joget (semicolon separated)
        FormRow result = new FormRow();
        String delimitedValue = FormUtil.generateElementPropertyValues(finalFiles.toArray(new String[0]));
        result.setProperty(id, (delimitedValue != null) ? delimitedValue : "");
        rowSet.add(result);

        // also clear/reset _path if needed (templates rely on it)
        formData.addRequestParameterValues(id + "_path", finalFiles.toArray(new String[0]));

        return rowSet;
    }

    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("download".equals(action)) {



            String params = SecurityUtil.decrypt(request.getParameter("params"));
            JSONObject jsonParams = new JSONObject(params);
            LogUtil.info("Json Params",jsonParams.toString());

            Map config= new HashMap();
            String client = jsonParams.getString("client");
            config.put("client",client);

            String filePath="";

            if(client.equalsIgnoreCase("SHAREPOINT")){
                String siteId = jsonParams.getString("siteId");
                String driveId = jsonParams.getString("driveId");
                //String sharePointPath = jsonParams.getString("sharePointPath");
                String itemId = jsonParams.getString("itemId");
                String clientId = jsonParams.getString("clientId");
                String clientSecret = jsonParams.getString("clientSecret");
                String tenantId = jsonParams.getString("tenantId");

                config.put("siteId",siteId);
                config.put("driveId",driveId);
                //config.put("sharePointPath",sharePointPath);
                //config.put("itemId",itemId);
                config.put("clientId",clientId);
                config.put("clientSecret",clientSecret);
                config.put("tenantId",tenantId);

                filePath=itemId;
            }else{
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }

            FileServiceUtil fileServiceUtil = new FileServiceUtil(config);
            String downloadLink = fileServiceUtil.downloadFile(filePath);

            LogUtil.info("Download: ",downloadLink);
            response.sendRedirect(downloadLink);
        }
        else if ("edit".equals(action)) {
            String params = SecurityUtil.decrypt(request.getParameter("params"));
            JSONObject jsonParams = new JSONObject(params);
            LogUtil.info("Json Params",jsonParams.toString());

            Map config= new HashMap();
            String client = jsonParams.getString("client");
            config.put("client",client);

            String filePath="";

            if(client.equalsIgnoreCase("SHAREPOINT")){
                String siteId = jsonParams.getString("siteId");
                String driveId = jsonParams.getString("driveId");
                //String sharePointPath = jsonParams.getString("sharePointPath");
                String itemId = jsonParams.getString("itemId");
                String clientId = jsonParams.getString("clientId");
                String clientSecret = jsonParams.getString("clientSecret");
                String tenantId = jsonParams.getString("tenantId");

                config.put("siteId",siteId);
                config.put("driveId",driveId);
                //config.put("sharePointPath",sharePointPath);
                //config.put("itemId",itemId);
                config.put("clientId",clientId);
                config.put("clientSecret",clientSecret);
                config.put("tenantId",tenantId);
                filePath=itemId;
            }else{
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
            FileServiceUtil fileServiceUtil = new FileServiceUtil(config);
            String downloadLink = fileServiceUtil.getEditLink(filePath);

            LogUtil.info("Edit Link: ",downloadLink);
            response.sendRedirect(downloadLink);
        } else {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    public  String resolveSafeUploadPath(String rawPath, FormData formData) {
        if (rawPath == null || rawPath.trim().isEmpty()) return "/";
        try {
            String path = rawPath.trim().replace("\\", "/");

            // Resolve {fieldId} using FormData
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

            // Resolve #hash.variable# using Joget Hash Variable processor
            path = AppUtil.processHashVariable(path, null, null, null);

            // Normalize path
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
        // Split the input based on "|"

        if(input==null) return resultMap;

        String[] parts = input.split("\\|");
        if (parts.length == 2) {
            String filename = parts[0].trim();
            String documentId = parts[1].trim();
            resultMap.put("filename", filename);
            resultMap.put("fileId", documentId);
        } else {
            LogUtil.info(getClassName(), "Invalid input format.");
        }

        return resultMap;
    }

}