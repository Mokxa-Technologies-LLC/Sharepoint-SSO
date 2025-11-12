package org.joget.mokxa.util;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.joget.commons.util.LogUtil;
import org.joget.mokxa.model.ApiResponse;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SharePointUtil {
    private final String tenantId;
    private final String clientId;
    private final String clientSecret;
    private String accessToken;

    public SharePointUtil(String tenantId, String clientId, String clientSecret) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getClassName() {
        return getClass().getName();
    }

    public ApiResponse authenticate() {
        ApiResponse apiResponse = new ApiResponse();
        LogUtil.info(getClass().getName(), "Authenticating with Microsoft Graph API...");
        String tokenUrl = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(tokenUrl);
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");

            String payload = "grant_type=client_credentials" +
                    "&client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&scope=https%3A%2F%2Fgraph.microsoft.com%2F.default";

            post.setEntity(new StringEntity(payload));

            try (CloseableHttpResponse response = client.execute(post)) {
                int code = response.getStatusLine().getStatusCode();
                apiResponse.setResponseCode(response.getStatusLine().getStatusCode());
                String body = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
                apiResponse.setResponseBody(body);

                if (code == 200) {
                    JSONObject json = new JSONObject(body);
                    accessToken = json.getString("access_token");
                    LogUtil.info(getClass().getName(), "Access token obtained successfully.");
                    return apiResponse;
                } else {
                    LogUtil.warn(getClass().getName(), "Authentication failed. Code: " + code + " Body: " + body);
                }
            }
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Error during authentication.");
        }
        return null;
    }

    private ApiResponse executeRequest(HttpUriRequest request) {
        ApiResponse apiResponse = new ApiResponse();
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

            if(accessToken==null){
                authenticate();
            }
            if (accessToken != null) {
                request.setHeader("Authorization", "Bearer " + accessToken);
            }
            request.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = client.execute(request)) {
                int code = response.getStatusLine().getStatusCode();
                apiResponse.setResponseCode(response.getStatusLine().getStatusCode());
                String body = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
                apiResponse.setResponseBody(body);
                LogUtil.info(getClass().getName(),
                        "Request [" + request.getMethod() + "] " + request.getURI() + " → Code: " + code+ " → Response: " + body);
                return apiResponse;
            }
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e,
                    "Error executing " + request.getMethod() + " request to " + request.getURI());
            return null;
        }
    }

    public ApiResponse createFolder(String siteId, String driveId,String parentPath, String folderName) {
        try {
            String endpoint;
            if (parentPath == null || parentPath.isEmpty() || "/".equals(parentPath)) {
                endpoint = String.format("https://graph.microsoft.com/v1.0/sites/%s/drives/%s/root/children", siteId, driveId);
            } else {
                endpoint = String.format("https://graph.microsoft.com/v1.0/sites/%s/drives/%s/root:/%s:/children", siteId, driveId, parentPath);
            }

            LogUtil.info(getClass().getName(), "Creating folder: " + folderName + " at path: " + parentPath);

            JSONObject json = new JSONObject()
                    .put("name", folderName)
                    .put("folder", new JSONObject())
                    .put("@microsoft.graph.conflictBehavior", "rename");

            HttpPost post = new HttpPost(endpoint);
            post.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

            LogUtil.info(getClass().getName(), "Creating folder: " + folderName);
            ApiResponse apiResponse = executeRequest(post);
            return apiResponse;
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Error creating folder: " + folderName);
            return null;
        }
    }

    public String createFile(String siteId, String driveId,String folderPath, File file) {
        try {
            // Ensure folder exists before upload
            if (folderPath != null && !folderPath.trim().isEmpty()) {
                folderPath= encodeFolderPath(folderPath);
                ensureFolderExists(siteId, driveId,folderPath);
            }

            long fileSize = file.length();
            final long FOUR_MB = 4 * 1024 * 1024;

            if (fileSize <= FOUR_MB) {
                // Small file upload (simple PUT)
                String endpoint = String.format(
                        "https://graph.microsoft.com/v1.0/sites/%s/drives/%s/root:%s/%s:/content",
                        siteId, driveId,
                        (folderPath == null || folderPath.isEmpty()) ? "" : "/" + folderPath,
                        URLEncoder.encode(file.getName(), StandardCharsets.UTF_8.toString())
                                .replace("+", "%20")
                );

                HttpPut put = new HttpPut(endpoint);
                put.setEntity(new FileEntity(file, ContentType.DEFAULT_BINARY));
                LogUtil.info(getClass().getName(), "Uploading small file: " + file.getName());

                ApiResponse apiResponse= executeRequest(put);
                String itemId =getItemIdFromApiResponse(apiResponse);
                return itemId;
            } else {
                // Large file upload (resumable session)
                LogUtil.info(getClass().getName(), "Starting resumable upload for large file: " + file.getName());

                String createSessionEndpoint = String.format(
                        "https://graph.microsoft.com/v1.0/sites/%s/drives/%s/root:%s/%s:/createUploadSession",
                        siteId, driveId,
                        (folderPath == null || folderPath.isEmpty()) ? "" : "/" + folderPath,
                        URLEncoder.encode(file.getName(), StandardCharsets.UTF_8.toString())
                                .replace("+", "%20")
                );

                // Step 1: Create upload session
                HttpPost post = new HttpPost(createSessionEndpoint);
                post.setHeader("Content-Type", "application/json");
                post.setEntity(new StringEntity("{}", ContentType.APPLICATION_JSON));
                ApiResponse sessionResp = executeRequest(post);

                if (sessionResp == null || sessionResp.getResponseCode() >= 300) {
                    LogUtil.error(getClass().getName(), null, "Failed to create upload session: " + sessionResp);
                    return null;
                }

                JSONObject json = new JSONObject(sessionResp.getResponseBody());
                String uploadUrl = json.optString("uploadUrl", null);
                if (uploadUrl == null || uploadUrl.isEmpty()) {
                    return null;
                }

                // Step 2: Upload file in chunks (approx 5MB each)
                try (InputStream inputStream = new FileInputStream(file)) {
                    final int CHUNK_SIZE = 5 * 1024 * 1024;
                    byte[] buffer = new byte[CHUNK_SIZE];
                    long uploaded = 0;
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        long start = uploaded;
                        long end = uploaded + bytesRead - 1;

                        HttpPut chunkPut = new HttpPut(uploadUrl);
                        chunkPut.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
                        chunkPut.setEntity(new ByteArrayEntity(Arrays.copyOf(buffer, bytesRead)));

                        ApiResponse chunkResp = executeRequest(chunkPut);
                        if (chunkResp == null || chunkResp.getResponseCode() >= 400) {
                            LogUtil.error(getClass().getName(), null, "Chunk upload failed: " + chunkResp);
                            return null;
                        }

                        uploaded += bytesRead;
                    }

                    LogUtil.info(getClass().getName(), "Resumable upload completed for file: " + file.getName());
                    String itemId =getItemIdFromApiResponse(sessionResp);
                    return itemId;
                }
            }

        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Error uploading file: " + file.getName());
            return null;
        }
    }

    public ApiResponse getFile(String siteId, String driveId, String itemId) {
        try {
            String endpoint = String.format(
                    //"https://graph.microsoft.com/v1.0/sites/%s/drives/%s/items/root:/%s",
                    "https://graph.microsoft.com/v1.0/sites/%s/drives/%s/items/%s",
                    siteId, driveId, itemId);

            HttpGet get = new HttpGet(endpoint);
            LogUtil.info(getClass().getName(), "Fetching file itemId: " + itemId);
            return executeRequest(get);
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Error fetching file itemId: " + itemId);
            return null;
        }
    }

    public ApiResponse deleteFile(String siteId, String driveId, String itemId) {
        try {
            String endpoint = String.format(
//                    "https://graph.microsoft.com/v1.0/sites/%s/drives/%s/items/root:%s",
                    "https://graph.microsoft.com/v1.0/sites/%s/drives/%s/items/%s",
                    siteId, driveId, itemId);

            HttpDelete delete = new HttpDelete(endpoint);
            LogUtil.info(getClass().getName(), "Deleting file itemId: " + itemId);
            return executeRequest(delete);
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Error deleting file itemId: " + itemId);
            return null;
        }
    }

    private void ensureFolderExists(String siteId, String driveId,String folderPath) throws IOException {
        if (folderPath == null || folderPath.trim().isEmpty()) {
            return;
        }

        String normalizedPath = folderPath.startsWith("/") ? folderPath : "/" + folderPath;
        LogUtil.info("Normalize path", normalizedPath);

        // Check if folder exists
        String checkUrl = String.format(
                "https://graph.microsoft.com/v1.0/sites/%s/drives/%s/root:%s",
                siteId, driveId, normalizedPath);

        HttpGet get = new HttpGet(checkUrl);
        ApiResponse response = executeRequest(get);

        if (response == null || response.getResponseCode() == 404) {
            int lastSlash = folderPath.lastIndexOf('/');

            String parent = (lastSlash > 0) ? folderPath.substring(0, lastSlash) : "";
            String name = (lastSlash >= 0) ? folderPath.substring(lastSlash + 1) : folderPath;

            parent = parent.replaceAll("^/+", "").replaceAll("/+$", "");
            name = name.replaceAll("^/+", "").replaceAll("/+$", "");

            LogUtil.info("Parent", parent);
            LogUtil.info("Name", name);

            if (!parent.isEmpty()) {
                ensureFolderExists(siteId, driveId,parent); // recursive call for parent
            }

            ApiResponse createRes = createFolder(siteId, driveId,parent, name);
            if (createRes == null || createRes.getResponseCode() >= 400) {
                throw new IOException("Failed to create folder: " + folderPath + " → " +
                        (createRes != null ? createRes.getResponseBody() : "No response"));
            }
            LogUtil.info(getClass().getName(), "Created folder: " + folderPath);
        }
    }

    public String getItemIdFromApiResponse(ApiResponse apiResponse) {
        try {
            if (apiResponse == null || apiResponse.getResponseBody() == null) {
                LogUtil.warn(getClassName(), "No API response provided to extract item ID.");
                return null;
            }
            JSONObject response = new JSONObject(apiResponse.getResponseBody());
            String itemId = response.optString("id", null);

            LogUtil.info(getClassName(), "Extracted drive item ID: " + itemId);
            return itemId;
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Failed to extract item ID from API response.");
            return null;
        }
    }

    public String getListItemIdByDriveItemId(String siteId, String driveId, String itemId) {
        try {
            String endpoint = String.format(
                    "https://graph.microsoft.com/v1.0/sites/%s/drives/%s/items/%s?$expand=listItem",
                    siteId, driveId, itemId
            );

            HttpGet get = new HttpGet(endpoint);
            ApiResponse resp = executeRequest(get);

            if (resp != null && resp.getResponseCode() == 200) {
                JSONObject json = new JSONObject(resp.getResponseBody());
                JSONObject listItem = json.optJSONObject("listItem");

                if (listItem != null) {
                    String listItemId = listItem.optString("id", null);
                    LogUtil.info(getClassName(), "Found listItem ID: " + listItemId + " for drive item: " + itemId);
                    return listItemId;
                } else {
                    LogUtil.warn(getClassName(), "listItem not found in expanded response for drive item: " + itemId);
                }
            } else {
                LogUtil.warn(getClassName(), "Failed to fetch listItem for drive item: " + itemId +
                        " → Code: " + (resp != null ? resp.getResponseCode() : 0));
            }
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Error fetching listItem for drive item: " + itemId);
        }
        return null;
    }

    public void updateItemMetadata(String siteId, String listId, String listItemId, JSONObject metadata) {
        try {
            if (listId == null || listItemId == null) {
                LogUtil.warn(getClassName(), "Missing listId or listItemId for metadata update.");
                return;
            }

            String endpoint = String.format(
                    "https://graph.microsoft.com/v1.0/sites/%s/lists/%s/items/%s/fields",
                    siteId, listId, listItemId
            );

            HttpPatch patch = new HttpPatch(endpoint);
            patch.setEntity(new StringEntity(metadata.toString(), ContentType.APPLICATION_JSON));

            LogUtil.info(getClassName(), "Updating metadata for item: " + listItemId + " → " + metadata.toString());

            ApiResponse resp = executeRequest(patch);
            if (resp != null) {
                LogUtil.info(getClassName(), "Metadata updated successfully. Code: " + resp.getResponseCode());
            } else {
                LogUtil.warn(getClassName(), "Metadata update returned null response.");
            }
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, "Error updating metadata for item: " + listItemId);
        }
    }

    public ApiResponse listFileVersions(String siteId, String driveId, String itemId) {
        try {
            String endpoint = String.format(
                    "https://graph.microsoft.com/v1.0/sites/%s/drives/%s/items/%s/versions",
                    siteId, driveId, itemId
            );

            LogUtil.info(getClass().getName(), "Fetching versions for itemId: " + itemId);

            HttpGet get = new HttpGet(endpoint);
            return executeRequest(get);

        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Error fetching versions for file: " + itemId);
            return null;
        }
    }

    public String getEditLink(String siteId, String driveId, String itemId) {
        try {
            String endpoint = String.format(
                    "https://graph.microsoft.com/v1.0/sites/%s/drives/%s/items/%s/createLink",
                    siteId, driveId, itemId
            );

            LogUtil.info(getClass().getName(), "Generating edit link for itemId: " + itemId);

            // Create POST request
            HttpPost post = new HttpPost(endpoint);
            post.setHeader("Content-Type", "application/json");

            // Body: request an editable link
            JSONObject body = new JSONObject();
            body.put("type", "edit");
            body.put("scope", "organization"); // or "users" / "anonymous" based on your permission policy
            post.setEntity(new StringEntity(body.toString(), "UTF-8"));

            // Execute API request using your shared helper
            ApiResponse response = executeRequest(post);

            if (response != null && response.getResponseCode() >=200 && response.getResponseCode()<300 ) {
                JSONObject json = new JSONObject(response.getResponseBody());
                if (json.has("link")) {
                    JSONObject link = json.getJSONObject("link");
                    String editUrl = link.optString("webUrl", null);
                    LogUtil.info(getClass().getName(), "Edit link generated: " + editUrl);
                    return editUrl;
                } else {
                    LogUtil.warn(getClass().getName(), "Edit link not found in response JSON.");
                }
            } else {
                LogUtil.warn(getClass().getName(), "Failed to get edit link. Code: " +
                        (response != null ? response.getResponseCode() : "null"));
            }

            return null;
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Error generating edit link for itemId: " + itemId);
            return null;
        }
    }

    private String encodeFolderPath(String folderPath) {
        if (folderPath == null || folderPath.isEmpty()) {
            return folderPath;
        }
        String[] parts = folderPath.split("/");
        return Arrays.stream(parts)
                .map(part -> {
                    try {
                        return URLEncoder.encode(part, StandardCharsets.UTF_8.toString())
                                .replace("+", "%20");
                    } catch (Exception e) {
                        LogUtil.error(getClass().getName(), e, "Error encoding folder path part: " + part);
                        return part;
                    }
                })
                .collect(Collectors.joining("/"));
    }



}
