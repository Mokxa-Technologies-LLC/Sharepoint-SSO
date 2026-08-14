<#--<div class="form-cell" ${elementMetaData!}>-->
<#--    <#if !(request.getAttribute("org.joget.apps.form.lib.FileUpload")?? || request.getAttribute("org.joget.plugin.enterprise.ImageUpload")??)  >-->
<#--        <link rel="stylesheet" href="${request.contextPath}/js/dropzone/dropzone.css" />-->
<#--        <script type="text/javascript" src="${request.contextPath}/js/dropzone/dropzone.js"></script>-->
<#--        <script src="${request.contextPath}/plugin/org.joget.apps.form.lib.FileUpload/js/jquery.fileupload.js"></script>-->
<#--        <script type="text/javascript">-->
<#--            Dropzone.autoDiscover = false;-->
<#--        </script>-->
<#--    </#if>-->

<#--    <label class="label" field-tooltip="${elementParamName!}">${element.properties.label} <span class="form-cell-validator">${decoration}</span><#if error??> <span class="form-error-message">${error}</span></#if></label>-->

<#--    <#assign readonly = (element.properties.readonly! == 'true')>-->

<#--    <div id="form-fileupload_${elementParamName!}_${element.properties.elementUniqueKey!}" tabindex="0" class="form-fileupload <#if error??>form-error-cell</#if> <#if readonly>readonly<#else>dropzone</#if>">-->
<#--        <#if !readonly>-->
<#--            <div class="dz-message needsclick">-->
<#--                Drop files here or click to upload.-->
<#--            </div>-->
<#--            <input style="display:none" id="${elementParamName!}" name="${elementParamName!}" type="file" size="${element.properties.size!}" <#if error??>class="form-error-cell"</#if> <#if element.properties.multiple! == 'true'>multiple</#if>/>-->
<#--        </#if>-->

<#--        <ul class="form-fileupload-value" id="form-fileupload-list_${elementParamName!}">-->
<#--            <#if !readonly>-->
<#--                <li class="template" style="display:none;">-->
<#--                    <span class="name" data-dz-name></span> <a class="remove" style="display:none">@@form.fileupload.remove@@</a>-->
<#--                    <strong class="error text-danger" data-dz-errormessage></strong>-->
<#--                    <div class="progress progress-striped active" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="0">-->
<#--                        <div class="progress-bar progress-bar-success" style="width:0%;" data-dz-uploadprogress></div>-->
<#--                    </div>-->
<#--                    <input type="hidden" name="${elementParamName!}_path" value="" disabled/>-->
<#--                </li>-->
<#--            </#if>-->

<#--            &lt;#&ndash; newly uploaded local temp files (not yet saved/remote) &ndash;&gt;-->
<#--            <#if tempFilePaths??>-->
<#--                <#list tempFilePaths?keys as key>-->
<#--                    <li>-->
<#--                        <span class="name">${tempFilePaths[key]!?html}</span>-->
<#--                        <#if !readonly>-->
<#--                            <a class="remove">@@form.fileupload.remove@@</a>-->
<#--                        </#if>-->
<#--                        <input type="hidden" name="${elementParamName!}_path" value="${key!?html}"/>-->
<#--                    </li>-->
<#--                </#list>-->
<#--            </#if>-->

<#--            &lt;#&ndash; existing stored files: rendered as placeholders immediately (filename visible),-->
<#--                 links populated asynchronously by JS below (works whether sync is on or off) &ndash;&gt;-->
<#--            <#if items??>-->
<#--                <#list items as item>-->
<#--                    <li class="file-item" data-filename="${item.filename!?html}" data-itemid="${item.itemId!?html}" style="position:relative;">-->
<#--                        <a href="javascript:void(0);" class="file-link-placeholder" title="Loading link...">-->
<#--                            <span class="name">${item.filename!?html}</span>-->
<#--                        </a>-->

<#--                        <#if !readonly>-->
<#--                            <div class="file-menu" style="display:inline-block; position:relative; margin-left:10px;">-->
<#--                                <button type="button" class="menu-trigger" style="background:none;border:none;cursor:pointer;font-size:18px;">⋮</button>-->
<#--                                <ul class="menu-dropdown" style="-->
<#--                                    display:none;-->
<#--                                    position:absolute;-->
<#--                                    top:25px;-->
<#--                                    right:0;-->
<#--                                    background:#fff;-->
<#--                                    list-style:none;-->
<#--                                    padding:5px 0;-->
<#--                                    margin:0;-->
<#--                                    border:1px solid #ddd;-->
<#--                                    border-radius:8px;-->
<#--                                    box-shadow:0 2px 8px rgba(0,0,0,0.15);-->
<#--                                    z-index:10000;-->
<#--                                    width:100px;-->
<#--                                    font-size:13px;-->
<#--                                ">-->
<#--                                    <li class="menu-item remove-file" style="padding:8px 12px;cursor:pointer;">Remove</li>-->
<#--                                    <li class="menu-item has-submenu" style="padding:8px 12px;cursor:pointer;position:relative;">Open ▸-->
<#--                                        <ul class="submenu" style="-->
<#--                                            display:none;-->
<#--                                            position:absolute;-->
<#--                                            top:0;-->
<#--                                            left:100px;-->
<#--                                            background:#fff;-->
<#--                                            border:1px solid #ddd;-->
<#--                                            border-radius:8px;-->
<#--                                            box-shadow:0 2px 8px rgba(0,0,0,0.15);-->
<#--                                            list-style:none;-->
<#--                                            margin:0;-->
<#--                                            padding:5px 0;-->
<#--                                            width:130px;-->
<#--                                        ">-->
<#--                                            <li class="submenu-item open-web" style="padding:8px 12px;cursor:pointer;">Open in Web</li>-->
<#--                                            <li class="submenu-item open-teams" style="padding:8px 12px;cursor:pointer;">Open in Teams</li>-->
<#--                                            <#if item.filename?lower_case?ends_with(".docx")>-->
<#--                                                <li class="submenu-item open-native" style="padding:8px 12px;cursor:pointer;">Open in Word</li>-->
<#--                                            </#if>-->
<#--                                        </ul>-->
<#--                                    </li>-->
<#--                                </ul>-->
<#--                            </div>-->
<#--                        </#if>-->

<#--                        <input type="hidden" name="${elementParamName!}_path" value="${item.rawValue!?html}"/>-->
<#--                        <input type="hidden" class="edit-web" value=""/>-->
<#--                        <input type="hidden" class="edit-teams" value=""/>-->
<#--                        <input type="hidden" class="edit-native" value=""/>-->
<#--                    </li>-->
<#--                </#list>-->
<#--            </#if>-->
<#--        </ul>-->

<#--        <div id="sp-status-banner_${elementParamName!}"-->
<#--             style="display:none;font-size:12px;padding:6px 10px;border-radius:4px;margin-top:6px;">-->
<#--        </div>-->

<#--        <#if syncEnabled?? && syncEnabled>-->
<#--            <div class="sp-sync-indicator" id="sp-sync-indicator_${elementParamName!}" style="font-size:12px;color:#888;margin-top:4px;">-->
<#--                Syncing in progress...-->
<#--            </div>-->
<#--        </#if>-->
<#--    </div>-->

<#--    <script>-->
<#--        $(document).ready(function(){-->

<#--            var $wrapper = $("#form-fileupload_${elementParamName!}_${element.properties.elementUniqueKey!}");-->
<#--            if ($wrapper.closest('.form-section, [style*="display:none"], .hidden').is(':hidden')) {-->
<#--                return;-->
<#--            }-->

<#--            <#if !readonly>-->
<#--            $('#form-fileupload_${elementParamName!}_${element.properties.elementUniqueKey!}').fileUploadField({-->
<#--                url : "${element.serviceUrl!}",-->
<#--                paramName : "${elementParamName!}",-->
<#--                multiple : "${element.properties.multiple!}",-->
<#--                maxSize : "${element.properties.maxSize!}",-->
<#--                maxSizeMsg : "${element.properties.maxSizeMsg!}",-->
<#--                fileType : "${element.properties.fileType!}",-->
<#--                fileTypeMsg : "${element.properties.fileTypeMsg!}",-->
<#--                padding : "${element.properties.padding!}",-->
<#--                removeFile : "${element.properties.removeFile!}",-->
<#--                resizeWidth : "${element.properties.resizeWidth!}",-->
<#--                resizeHeight : "${element.properties.resizeHeight!}",-->
<#--                resizeQuality : "${element.properties.resizeQuality!}",-->
<#--                resizeMethod : "${element.properties.resizeMethod!}"-->
<#--            });-->
<#--            </#if>-->

<#--            // ===== Dropdown / menu behaviour (delegated, works on dynamic content too) =====-->
<#--            $(document).on("click", ".menu-trigger", function(e) {-->
<#--                e.stopPropagation();-->
<#--                $(".menu-dropdown").hide();-->
<#--                $(this).siblings(".menu-dropdown").toggle();-->
<#--            });-->

<#--            $(document).on("mouseenter", ".has-submenu", function() {-->
<#--                $(this).children(".submenu").show();-->
<#--            }).on("mouseleave", ".has-submenu", function() {-->
<#--                $(this).children(".submenu").hide();-->
<#--            });-->

<#--            $(document).on("click", function() {-->
<#--                $(".menu-dropdown").hide();-->
<#--            });-->

<#--            $(document).on("click", ".remove-file", function() {-->
<#--                $(this).closest("li.file-item").remove();-->
<#--            });-->

<#--            $(document).on("click", ".submenu-item", function(e) {-->
<#--                e.stopPropagation();-->
<#--                const parent = $(this).closest("li.file-item");-->
<#--                const webUrl = parent.find(".edit-web").val();-->
<#--                const teamsUrl = parent.find(".edit-teams").val();-->
<#--                const nativeUrl = parent.find(".edit-native").val();-->

<#--                if ($(this).hasClass("open-web")) {-->
<#--                    window.open(webUrl, "_blank");-->
<#--                } else if ($(this).hasClass("open-teams")) {-->
<#--                    window.open(teamsUrl, "_blank");-->
<#--                } else if ($(this).hasClass("open-native")) {-->
<#--                    window.location.href = nativeUrl;-->
<#--                }-->

<#--                $(".menu-dropdown").hide();-->
<#--            });-->

<#--            // ===== Async link rendering / sync logic =====-->
<#--            var $listContainer = $("#form-fileupload-list_${elementParamName!}");-->
<#--            var $dropzone = $("#form-fileupload_${elementParamName!}_${element.properties.elementUniqueKey!}");-->
<#--            var $banner = $("#sp-status-banner_${elementParamName!}");-->
<#--            var contextPath = "${request.contextPath}";-->
<#--            var renderServiceUrl = "${renderServiceUrl!}";-->
<#--            var checkAuthServiceUrl = "${checkAuthServiceUrl!}";-->
<#--            var syncEnabled = ${(syncEnabled?? && syncEnabled)?then('true','false')};-->
<#--            <#if syncEnabled?? && syncEnabled>-->
<#--            var syncServiceUrl = "${syncServiceUrl!}";-->
<#--            </#if>-->


<#--            // ---- UI State Helpers ------>

<#--            function showBanner(msg, type) {-->
<#--                // type: 'error' | 'warn' | 'info'-->
<#--                var colors = {-->
<#--                    error: { bg: '#fdecea', color: '#c0392b', border: '#f5c6c2' },-->
<#--                    warn:  { bg: '#fff8e1', color: '#856404', border: '#ffe082' },-->
<#--                    info:  { bg: '#e8f4fd', color: '#0c5460', border: '#bee5eb' }-->
<#--                };-->
<#--                var c = colors[type] || colors.info;-->
<#--                $banner-->
<#--                    .text(msg)-->
<#--                    .css({ background: c.bg, color: c.color, border: '1px solid ' + c.border, display: 'block' });-->
<#--            }-->

<#--            function hideBanner() {-->
<#--                $banner.hide();-->
<#--            }-->

<#--            function disableFileActions(reason) {-->
<#--                // Disable dropzone upload-->
<#--                $dropzone.addClass('sp-disabled');-->
<#--                $dropzone.find('.dz-message').hide();-->
<#--                $dropzone.find('input[type="file"]').prop('disabled', true);-->

<#--                // Disable all menu actions-->
<#--                $listContainer.find('.menu-trigger').prop('disabled', true).css('opacity', '0.4');-->

<#--                // Make all file links non-clickable-->
<#--                $listContainer.find('.file-link-placeholder').removeAttr('href').css('cursor', 'default').attr('title', reason);-->

<#--                showBanner(reason, 'error');-->
<#--            }-->

<#--            function enableFileActions() {-->
<#--                $dropzone.removeClass('sp-disabled');-->
<#--                $dropzone.find('.dz-message').show();-->
<#--                $dropzone.find('input[type="file"]').prop('disabled', false);-->
<#--                $listContainer.find('.menu-trigger').prop('disabled', false).css('opacity', '1');-->
<#--                hideBanner();-->
<#--            }-->

<#--            function applyLinksToExistingItems(linksMap) {-->
<#--                $listContainer.find("li.file-item").each(function() {-->
<#--                    var $li = $(this);-->
<#--                    var itemId = $li.data("itemid");-->
<#--                    if (!itemId || !linksMap[itemId]) return;-->

<#--                    var info = linksMap[itemId];-->
<#--                    $li.find(".file-link-placeholder")-->
<#--                        .attr("href", contextPath + info.downloadUrl)-->
<#--                        .attr("target", "_blank")-->
<#--                        .removeAttr("title")-->
<#--                        .css('cursor', 'pointer');-->
<#--                    $li.find(".edit-web").val(contextPath + info.editWebUrl);-->
<#--                    $li.find(".edit-teams").val(contextPath + info.editTeamsUrl);-->
<#--                    $li.find(".edit-native").val(contextPath + info.editNativeUrl);-->

<#--                    // Re-enable menu trigger for this item-->
<#--                    $li.find('.menu-trigger').prop('disabled', false).css('opacity', '1');-->
<#--                });-->
<#--            }-->


<#--            function renderLinksForItemIds(itemIds, onError) {-->
<#--                if (!itemIds || !itemIds.length) return;-->
<#--                $.ajax({-->
<#--                    url: contextPath + renderServiceUrl + "&itemIds=" + encodeURIComponent(itemIds.join(",")),-->
<#--                    method: "GET",-->
<#--                    dataType: "json",-->
<#--                    success: function(resp) {-->
<#--                        if (resp.status === "ok" && resp.links) {-->
<#--                            applyLinksToExistingItems(resp.links);-->
<#--                        } else {-->
<#--                            if (onError) onError(resp.message || "Failed to load file links.");-->
<#--                        }-->
<#--                    },-->
<#--                    error: function(xhr) {-->
<#--                        var msg = "Unable to load file links. Please refresh the page.";-->
<#--                        try { msg = JSON.parse(xhr.responseText).message || msg; } catch(e) {}-->
<#--                        if (onError) onError(msg);-->
<#--                    }-->
<#--                });-->
<#--            }-->



<#--            function rebuildFileListFromSync(filesMap) {-->
<#--                $listContainer.find("li.file-item").remove();-->
<#--                var itemIds = [];-->

<#--                $.each(filesMap, function(filename, itemId) {-->
<#--                    itemIds.push(itemId);-->
<#--                    var lower = filename.toLowerCase();-->
<#--                    var nativeMenuItem = lower.endsWith(".docx")-->
<#--                        ? '<li class="submenu-item open-native" style="padding:8px 12px;cursor:pointer;">Open in Word</li>' : '';-->

<#--                    var menuHtml = "";-->
<#--                    <#if !readonly>-->
<#--                    menuHtml =-->
<#--                        '<div class="file-menu" style="display:inline-block;position:relative;margin-left:10px;">' +-->
<#--                        '<button type="button" class="menu-trigger" style="background:none;border:none;cursor:pointer;font-size:18px;">⋮</button>' +-->
<#--                        '<ul class="menu-dropdown" style="display:none;position:absolute;top:25px;right:0;background:#fff;list-style:none;padding:5px 0;margin:0;border:1px solid #ddd;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.15);z-index:10000;width:100px;font-size:13px;">' +-->
<#--                        '<li class="menu-item remove-file" style="padding:8px 12px;cursor:pointer;">Remove</li>' +-->
<#--                        '<li class="menu-item has-submenu" style="padding:8px 12px;cursor:pointer;position:relative;">Open ▸' +-->
<#--                        '<ul class="submenu" style="display:none;position:absolute;top:0;left:100px;background:#fff;border:1px solid #ddd;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.15);list-style:none;margin:0;padding:5px 0;width:130px;">' +-->
<#--                        '<li class="submenu-item open-web" style="padding:8px 12px;cursor:pointer;">Open in Web</li>' +-->
<#--                        '<li class="submenu-item open-teams" style="padding:8px 12px;cursor:pointer;">Open in Teams</li>' +-->
<#--                        nativeMenuItem +-->
<#--                        '</ul></li></ul></div>';-->
<#--                    </#if>-->

<#--                    var $li = $(-->
<#--                        '<li class="file-item" data-filename="' + filename + '" data-itemid="' + itemId + '" style="position:relative;">' +-->
<#--                        '<a href="javascript:void(0);" class="file-link-placeholder" title="Loading link..."><span class="name">' + filename + '</span></a>' +-->
<#--                        menuHtml +-->
<#--                        '<input type="hidden" name="${elementParamName!}_path" value="' + filename + '|' + itemId + '"/>' +-->
<#--                        '<input type="hidden" class="edit-web" value=""/>' +-->
<#--                        '<input type="hidden" class="edit-teams" value=""/>' +-->
<#--                        '<input type="hidden" class="edit-native" value=""/>' +-->
<#--                        '</li>'-->
<#--                    );-->
<#--                    $listContainer.append($li);-->
<#--                });-->

<#--                renderLinksForItemIds(itemIds, function(errMsg) {-->
<#--                    showBanner("Files loaded but links could not be fetched: " + errMsg, 'warn');-->
<#--                });-->
<#--            }-->

<#--            // ---- Click feedback for items with no link yet ------>
<#--            $(document).on("click", ".file-link-placeholder", function(e) {-->
<#--                if ($(this).attr('href') === 'javascript:void(0)' || !$(this).attr('href')) {-->
<#--                    e.preventDefault();-->
<#--                    showBanner("Link is still loading, please try again in a moment.", 'info');-->
<#--                }-->
<#--            });-->

<#--            // ---- Main: Auth check gates everything ------>
<#--            $.ajax({-->
<#--                url: contextPath + checkAuthServiceUrl,-->
<#--                method: "GET",-->
<#--                dataType: "json",-->
<#--                success: function(authResp) {-->
<#--                    if (authResp.status !== "ok") {-->
<#--                        disableFileActions(authResp.message || "SharePoint authentication failed. Actions are disabled.");-->
<#--                        return;-->
<#--                    }-->

<#--                    // Auth OK — proceed with sync or direct render-->
<#--                    <#if syncEnabled?? && syncEnabled>-->
<#--                    var $indicator = $("#sp-sync-indicator_${elementParamName!}");-->
<#--                    $.ajax({-->
<#--                        url: contextPath + syncServiceUrl,-->
<#--                        method: "GET",-->
<#--                        dataType: "json",-->
<#--                        success: function(resp) {-->
<#--                            $indicator.hide();-->
<#--                            if (resp.status === "ok" && resp.files) {-->
<#--                                if (resp.changed) {-->
<#--                                    rebuildFileListFromSync(resp.files);-->
<#--                                } else {-->
<#--                                    var existingItemIds = [];-->
<#--                                    $listContainer.find("li.file-item").each(function() {-->
<#--                                        var id = $(this).data("itemid");-->
<#--                                        if (id) existingItemIds.push(id);-->
<#--                                    });-->
<#--                                    renderLinksForItemIds(existingItemIds, function(errMsg) {-->
<#--                                        showBanner("Files loaded but links could not be fetched: " + errMsg, 'warn');-->
<#--                                    });-->
<#--                                }-->
<#--                            } else {-->
<#--                                $indicator.hide();-->
<#--                                disableFileActions(resp.message || "Sync failed. File actions are disabled.");-->
<#--                            }-->
<#--                        },-->
<#--                        error: function(xhr) {-->
<#--                            $indicator.hide();-->
<#--                            disableFileActions("Sync request failed. File actions are disabled. Please refresh.");-->
<#--                        }-->
<#--                    });-->
<#--                    <#else>-->
<#--                    var existingItemIds = [];-->
<#--                    $listContainer.find("li.file-item").each(function() {-->
<#--                        var id = $(this).data("itemid");-->
<#--                        if (id) existingItemIds.push(id);-->
<#--                    });-->
<#--                    renderLinksForItemIds(existingItemIds, function(errMsg) {-->
<#--                        showBanner("Files loaded but links could not be fetched: " + errMsg, 'warn');-->
<#--                    });-->
<#--                    </#if>-->
<#--                },-->
<#--                error: function() {-->
<#--                    disableFileActions("Unable to reach SharePoint. Upload and file actions are disabled.");-->
<#--                }-->
<#--            });-->

<#--        });-->
<#--    </script>-->
<#--</div>-->



<div class="form-cell" ${elementMetaData!}>
    <#if !(request.getAttribute("org.joget.apps.form.lib.FileUpload")?? || request.getAttribute("org.joget.plugin.enterprise.ImageUpload")??)  >
        <link rel="stylesheet" href="${request.contextPath}/js/dropzone/dropzone.css" />
        <script type="text/javascript" src="${request.contextPath}/js/dropzone/dropzone.js"></script>
        <script src="${request.contextPath}/plugin/org.joget.apps.form.lib.FileUpload/js/jquery.fileupload.js"></script>
        <script type="text/javascript">
            Dropzone.autoDiscover = false;
        </script>
    </#if>

    <label class="label" field-tooltip="${elementParamName!}">${element.properties.label} <span class="form-cell-validator">${decoration}</span><#if error??> <span class="form-error-message">${error}</span></#if></label>

    <#assign readonly = (element.properties.readonly! == 'true')>

    <div id="form-fileupload_${uniqueSuffix!}" tabindex="0" class="form-fileupload <#if error??>form-error-cell</#if> <#if readonly>readonly<#else>dropzone</#if>">
        <#if !readonly>
            <div class="dz-message needsclick">
                Drop files here or click to upload.
            </div>
            <input style="display:none" id="${elementParamName!}" name="${elementParamName!}" type="file" size="${element.properties.size!}" <#if error??>class="form-error-cell"</#if> <#if element.properties.multiple! == 'true'>multiple</#if>/>
        </#if>

        <ul class="form-fileupload-value" id="form-fileupload-list_${uniqueSuffix!}">
            <#if !readonly>
                <li class="template" style="display:none;">
                    <span class="name" data-dz-name></span> <a class="remove" style="display:none">@@form.fileupload.remove@@</a>
                    <strong class="error text-danger" data-dz-errormessage></strong>
                    <div class="progress progress-striped active" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="0">
                        <div class="progress-bar progress-bar-success" style="width:0%;" data-dz-uploadprogress></div>
                    </div>
                    <input type="hidden" name="${elementParamName!}_path" value="" disabled/>
                </li>
            </#if>

            <#-- newly uploaded local temp files (not yet saved/remote) -->
            <#if tempFilePaths??>
                <#list tempFilePaths?keys as key>
                    <li>
                        <span class="name">${tempFilePaths[key]!?html}</span>
                        <#if !readonly>
                            <a class="remove">@@form.fileupload.remove@@</a>
                        </#if>
                        <input type="hidden" name="${elementParamName!}_path" value="${key!?html}"/>
                    </li>
                </#list>
            </#if>

            <#-- existing stored files: rendered as placeholders immediately (filename visible),
                 links populated asynchronously by JS below (works whether sync is on or off) -->
            <#if items??>
                <#list items as item>
                    <li class="file-item" data-filename="${item.filename!?html}" data-itemid="${item.itemId!?html}" style="position:relative;">
                        <a href="javascript:void(0);" class="file-link-placeholder" title="Loading link...">
                            <span class="name">${item.filename!?html}</span>
                        </a>

                        <#if !readonly>
                            <div class="file-menu" style="display:inline-block; position:relative; margin-left:10px;">
                                <button type="button" class="menu-trigger" style="background:none;border:none;cursor:pointer;font-size:18px;">⋮</button>
                                <ul class="menu-dropdown" style="
                                    display:none;
                                    position:absolute;
                                    top:25px;
                                    right:0;
                                    background:#fff;
                                    list-style:none;
                                    padding:5px 0;
                                    margin:0;
                                    border:1px solid #ddd;
                                    border-radius:8px;
                                    box-shadow:0 2px 8px rgba(0,0,0,0.15);
                                    z-index:10000;
                                    width:100px;
                                    font-size:13px;
                                ">
                                    <li class="menu-item remove-file" style="padding:8px 12px;cursor:pointer;">Remove</li>
                                    <li class="menu-item has-submenu" style="padding:8px 12px;cursor:pointer;position:relative;">Open ▸
                                        <ul class="submenu" style="
                                            display:none;
                                            position:absolute;
                                            top:0;
                                            left:100px;
                                            background:#fff;
                                            border:1px solid #ddd;
                                            border-radius:8px;
                                            box-shadow:0 2px 8px rgba(0,0,0,0.15);
                                            list-style:none;
                                            margin:0;
                                            padding:5px 0;
                                            width:130px;
                                        ">
                                            <li class="submenu-item open-web" style="padding:8px 12px;cursor:pointer;">Open in Web</li>
                                            <li class="submenu-item open-teams" style="padding:8px 12px;cursor:pointer;">Open in Teams</li>
                                            <#if item.filename?lower_case?ends_with(".docx")>
                                                <li class="submenu-item open-native" style="padding:8px 12px;cursor:pointer;">Open in Word</li>
                                            </#if>
                                        </ul>
                                    </li>
                                </ul>
                            </div>
                        </#if>

                        <input type="hidden" name="${elementParamName!}_path" value="${item.rawValue!?html}"/>
                        <input type="hidden" class="edit-web" value=""/>
                        <input type="hidden" class="edit-teams" value=""/>
                        <input type="hidden" class="edit-native" value=""/>
                    </li>
                </#list>
            </#if>
        </ul>

        <div id="sp-status-banner_${uniqueSuffix!}"
             style="display:none;font-size:12px;padding:6px 10px;border-radius:4px;margin-top:6px;">
        </div>

        <#if syncEnabled?? && syncEnabled>
            <div class="sp-sync-indicator" id="sp-sync-indicator_${uniqueSuffix!}" style="font-size:12px;color:#888;margin-top:4px;">
                Syncing in progress...
            </div>
        </#if>
    </div>

    <script>
        $(document).ready(function(){

            var $wrapper = $("#form-fileupload_${uniqueSuffix!}");
            if ($wrapper.closest('.form-section, [style*="display:none"], .hidden').is(':hidden')) {
                return;
            }

            <#if !readonly>
            $('#form-fileupload_${uniqueSuffix!}').fileUploadField({
                url : "${element.serviceUrl!}",
                paramName : "${elementParamName!}",
                multiple : "${element.properties.multiple!}",
                maxSize : "${element.properties.maxSize!}",
                maxSizeMsg : "${element.properties.maxSizeMsg!}",
                fileType : "${element.properties.fileType!}",
                fileTypeMsg : "${element.properties.fileTypeMsg!}",
                padding : "${element.properties.padding!}",
                removeFile : "${element.properties.removeFile!}",
                resizeWidth : "${element.properties.resizeWidth!}",
                resizeHeight : "${element.properties.resizeHeight!}",
                resizeQuality : "${element.properties.resizeQuality!}",
                resizeMethod : "${element.properties.resizeMethod!}"
            });
            </#if>

            // ===== Dropdown / menu behaviour (delegated, works on dynamic content too) =====
            $(document).on("click", ".menu-trigger", function(e) {
                e.stopPropagation();
                $(".menu-dropdown").hide();
                $(this).siblings(".menu-dropdown").toggle();
            });

            $(document).on("mouseenter", ".has-submenu", function() {
                $(this).children(".submenu").show();
            }).on("mouseleave", ".has-submenu", function() {
                $(this).children(".submenu").hide();
            });

            $(document).on("click", function() {
                $(".menu-dropdown").hide();
            });

            $(document).on("click", ".remove-file", function() {
                $(this).closest("li.file-item").remove();
            });

            $(document).on("click", ".submenu-item", function(e) {
                e.stopPropagation();
                const parent = $(this).closest("li.file-item");
                const webUrl = parent.find(".edit-web").val();
                const teamsUrl = parent.find(".edit-teams").val();
                const nativeUrl = parent.find(".edit-native").val();

                if ($(this).hasClass("open-web")) {
                    window.open(webUrl, "_blank");
                } else if ($(this).hasClass("open-teams")) {
                    window.open(teamsUrl, "_blank");
                } else if ($(this).hasClass("open-native")) {
                    window.location.href = nativeUrl;
                }

                $(".menu-dropdown").hide();
            });

            // ===== Async link rendering / sync logic =====
            var $listContainer = $("#form-fileupload-list_${uniqueSuffix!}");
            var $dropzone = $("#form-fileupload_${uniqueSuffix!}");
            var $banner = $("#sp-status-banner_${uniqueSuffix!}");
            var contextPath = "${request.contextPath}";
            var renderServiceUrl = "${renderServiceUrl!}";
            var checkAuthServiceUrl = "${checkAuthServiceUrl!}";
            var syncEnabled = ${(syncEnabled?? && syncEnabled)?then('true','false')};
            <#if syncEnabled?? && syncEnabled>
            var syncServiceUrl = "${syncServiceUrl!}";
            </#if>


            // ---- UI State Helpers ----

            function showBanner(msg, type) {
                // type: 'error' | 'warn' | 'info'
                var colors = {
                    error: { bg: '#fdecea', color: '#c0392b', border: '#f5c6c2' },
                    warn:  { bg: '#fff8e1', color: '#856404', border: '#ffe082' },
                    info:  { bg: '#e8f4fd', color: '#0c5460', border: '#bee5eb' }
                };
                var c = colors[type] || colors.info;
                $banner
                    .text(msg)
                    .css({ background: c.bg, color: c.color, border: '1px solid ' + c.border, display: 'block' });
            }

            function hideBanner() {
                $banner.hide();
            }

            function disableFileActions(reason) {
                // Disable dropzone upload
                $dropzone.addClass('sp-disabled');
                $dropzone.find('.dz-message').hide();
                $dropzone.find('input[type="file"]').prop('disabled', true);

                // Disable all menu actions
                $listContainer.find('.menu-trigger').prop('disabled', true).css('opacity', '0.4');

                // Make all file links non-clickable
                $listContainer.find('.file-link-placeholder').removeAttr('href').css('cursor', 'default').attr('title', reason);

                showBanner(reason, 'error');
            }

            function enableFileActions() {
                $dropzone.removeClass('sp-disabled');
                $dropzone.find('.dz-message').show();
                $dropzone.find('input[type="file"]').prop('disabled', false);
                $listContainer.find('.menu-trigger').prop('disabled', false).css('opacity', '1');
                hideBanner();
            }

            function applyLinksToExistingItems(linksMap) {
                $listContainer.find("li.file-item").each(function() {
                    var $li = $(this);
                    var itemId = $li.data("itemid");
                    if (!itemId || !linksMap[itemId]) return;

                    var info = linksMap[itemId];
                    $li.find(".file-link-placeholder")
                        .attr("href", contextPath + info.downloadUrl)
                        .attr("target", "_blank")
                        .removeAttr("title")
                        .css('cursor', 'pointer');
                    $li.find(".edit-web").val(contextPath + info.editWebUrl);
                    $li.find(".edit-teams").val(contextPath + info.editTeamsUrl);
                    $li.find(".edit-native").val(contextPath + info.editNativeUrl);

                    // Re-enable menu trigger for this item
                    $li.find('.menu-trigger').prop('disabled', false).css('opacity', '1');
                });
            }


            function renderLinksForItemIds(itemIds, onError) {
                if (!itemIds || !itemIds.length) return;
                $.ajax({
                    url: contextPath + renderServiceUrl + "&itemIds=" + encodeURIComponent(itemIds.join(",")),
                    method: "GET",
                    dataType: "json",
                    success: function(resp) {
                        if (resp.status === "ok" && resp.links) {
                            applyLinksToExistingItems(resp.links);
                        } else {
                            if (onError) onError(resp.message || "Failed to load file links.");
                        }
                    },
                    error: function(xhr) {
                        var msg = "Unable to load file links. Please refresh the page.";
                        try { msg = JSON.parse(xhr.responseText).message || msg; } catch(e) {}
                        if (onError) onError(msg);
                    }
                });
            }



            function rebuildFileListFromSync(filesMap) {
                $listContainer.find("li.file-item").remove();
                var itemIds = [];

                $.each(filesMap, function(filename, itemId) {
                    itemIds.push(itemId);
                    var lower = filename.toLowerCase();
                    var nativeMenuItem = lower.endsWith(".docx")
                        ? '<li class="submenu-item open-native" style="padding:8px 12px;cursor:pointer;">Open in Word</li>' : '';

                    var menuHtml = "";
                    <#if !readonly>
                    menuHtml =
                        '<div class="file-menu" style="display:inline-block;position:relative;margin-left:10px;">' +
                        '<button type="button" class="menu-trigger" style="background:none;border:none;cursor:pointer;font-size:18px;">⋮</button>' +
                        '<ul class="menu-dropdown" style="display:none;position:absolute;top:25px;right:0;background:#fff;list-style:none;padding:5px 0;margin:0;border:1px solid #ddd;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.15);z-index:10000;width:100px;font-size:13px;">' +
                        '<li class="menu-item remove-file" style="padding:8px 12px;cursor:pointer;">Remove</li>' +
                        '<li class="menu-item has-submenu" style="padding:8px 12px;cursor:pointer;position:relative;">Open ▸' +
                        '<ul class="submenu" style="display:none;position:absolute;top:0;left:100px;background:#fff;border:1px solid #ddd;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.15);list-style:none;margin:0;padding:5px 0;width:130px;">' +
                        '<li class="submenu-item open-web" style="padding:8px 12px;cursor:pointer;">Open in Web</li>' +
                        '<li class="submenu-item open-teams" style="padding:8px 12px;cursor:pointer;">Open in Teams</li>' +
                        nativeMenuItem +
                        '</ul></li></ul></div>';
                    </#if>

                    var $li = $(
                        '<li class="file-item" data-filename="' + filename + '" data-itemid="' + itemId + '" style="position:relative;">' +
                        '<a href="javascript:void(0);" class="file-link-placeholder" title="Loading link..."><span class="name">' + filename + '</span></a>' +
                        menuHtml +
                        '<input type="hidden" name="${elementParamName!}_path" value="' + filename + '|' + itemId + '"/>' +
                        '<input type="hidden" class="edit-web" value=""/>' +
                        '<input type="hidden" class="edit-teams" value=""/>' +
                        '<input type="hidden" class="edit-native" value=""/>' +
                        '</li>'
                    );
                    $listContainer.append($li);
                });

                renderLinksForItemIds(itemIds, function(errMsg) {
                    showBanner("Files loaded but links could not be fetched: " + errMsg, 'warn');
                });
            }

            // ---- Click feedback for items with no link yet ----
            $(document).on("click", ".file-link-placeholder", function(e) {
                if ($(this).attr('href') === 'javascript:void(0)' || !$(this).attr('href')) {
                    e.preventDefault();
                    showBanner("Link is still loading, please try again in a moment.", 'info');
                }
            });

            // ---- Main: Auth check gates everything ----
            $.ajax({
                url: contextPath + checkAuthServiceUrl,
                method: "GET",
                dataType: "json",
                success: function(authResp) {
                    if (authResp.status !== "ok") {
                        disableFileActions(authResp.message || "SharePoint authentication failed. Actions are disabled.");
                        return;
                    }

                    // Auth OK — proceed with sync or direct render
                    <#if syncEnabled?? && syncEnabled>
                    var $indicator = $("#sp-sync-indicator_${uniqueSuffix!}");
                    $.ajax({
                        url: contextPath + syncServiceUrl,
                        method: "GET",
                        dataType: "json",
                        success: function(resp) {
                            $indicator.hide();
                            if (resp.status === "ok" && resp.files) {
                                if (resp.changed) {
                                    rebuildFileListFromSync(resp.files);
                                } else {
                                    var existingItemIds = [];
                                    $listContainer.find("li.file-item").each(function() {
                                        var id = $(this).data("itemid");
                                        if (id) existingItemIds.push(id);
                                    });
                                    renderLinksForItemIds(existingItemIds, function(errMsg) {
                                        showBanner("Files loaded but links could not be fetched: " + errMsg, 'warn');
                                    });
                                }
                            } else {
                                $indicator.hide();
                                disableFileActions(resp.message || "Sync failed. File actions are disabled.");
                            }
                        },
                        error: function(xhr) {
                            $indicator.hide();
                            disableFileActions("Sync request failed. File actions are disabled. Please refresh.");
                        }
                    });
                    <#else>
                    var existingItemIds = [];
                    $listContainer.find("li.file-item").each(function() {
                        var id = $(this).data("itemid");
                        if (id) existingItemIds.push(id);
                    });
                    renderLinksForItemIds(existingItemIds, function(errMsg) {
                        showBanner("Files loaded but links could not be fetched: " + errMsg, 'warn');
                    });
                    </#if>
                },
                error: function() {
                    disableFileActions("Unable to reach SharePoint. Upload and file actions are disabled.");
                }
            });

        });
    </script>
</div>