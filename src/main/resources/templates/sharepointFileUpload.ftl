<div class="form-cell" ${elementMetaData!}>
    <#if !(request.getAttribute("org.joget.apps.form.lib.FileUpload")?? || request.getAttribute("org.joget.plugin.enterprise.ImageUpload")??)  >
        <link rel="stylesheet" href="${request.contextPath}/js/dropzone/dropzone.css" />
        <script type="text/javascript" src="${request.contextPath}/js/dropzone/dropzone.js"></script>
        <script src="${request.contextPath}/plugin/org.joget.apps.form.lib.FileUpload/js/jquery.fileupload.js"></script>
        <script type="text/javascript">// Immediately after the js include
            Dropzone.autoDiscover = false;
        </script>
    </#if>

    <label class="label" field-tooltip="${elementParamName!}">${element.properties.label} <span class="form-cell-validator">${decoration}</span><#if error??> <span class="form-error-message">${error}</span></#if></label>
    <div id="form-fileupload_${elementParamName!}_${element.properties.elementUniqueKey!}" tabindex="0" class="form-fileupload <#if error??>form-error-cell</#if> <#if element.properties.readonly! == 'true'>readonly<#else>dropzone</#if>">
    <#if element.properties.readonly! != 'true'>
        <div class="dz-message needsclick">
           Drop files here or click to upload.
        </div>
        <input style="display:none" id="${elementParamName!}" name="${elementParamName!}" type="file" size="${element.properties.size!}" <#if error??>class="form-error-cell"</#if> <#if element.properties.multiple! == 'true'>multiple</#if>/>
    </#if>
        <ul class="form-fileupload-value">
            <#if element.properties.readonly! != 'true'>
                <li class="template" style="display:none;">
                    <span class="name" data-dz-name></span> <a class="remove"style="display:none">@@form.fileupload.remove@@</a>
                    <strong class="error text-danger" data-dz-errormessage></strong>
                    <div class="progress progress-striped active" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="0">
                        <div class="progress-bar progress-bar-success" style="width:0%;" data-dz-uploadprogress></div>
                    </div>
                    <input type="hidden" name="${elementParamName!}_path" value="" disabled/>
                </li>
            </#if>
            <#if tempFilePaths??>
                <#list tempFilePaths?keys as key>
                    <li>
                        <span class="name">${tempFilePaths[key]!?html}</span>
                            <#if element.properties.readonly! != 'true'>
                                <a class="remove">@@form.fileupload.remove@@</a>
                            </#if>
                        <input type="hidden" name="${elementParamName!}_path" value="${key!?html}"/>
                    </li>
                </#list>
            </#if>
           <#if filePaths??>
               <#list filePaths?keys as key>
                   <li class="file-item" data-filename="${filePaths[key]!?html}" style="position:relative;">
                       <a href="${request.contextPath}${key!?html}" target="_blank">
                           <span class="name">${filePaths[key]!?html}</span>
                       </a>

                       <#if element.properties.readonly! != 'true'>
                           <!-- 3-dot menu -->
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
                                           <li class="submenu-item open-native" style="padding:8px 12px;cursor:pointer;">Open in Word</li>
                                       </ul>
                                   </li>
                               </ul>
                           </div>
                       </#if>

                       <input type="hidden" name="${elementParamName!}_path" value="${filePaths[key]!?html}"/>

                       <!-- Hidden edit links for JS -->
                       <input type="hidden" class="edit-web" value="${request.contextPath}${editLinks[filePaths[key]!?html]!}"/>
                       <input type="hidden" class="edit-teams" value="${request.contextPath}${editTeamsLinks[filePaths[key]!?html]!}"/>
                       <input type="hidden" class="edit-native" value="${request.contextPath}${editNativeLinks[filePaths[key]!?html]!}"/>
                   </li>
               </#list>
           </#if>

        </ul>
    </div>
    <#if element.properties.readonly! != 'true'>
        <script>
            $(document).ready(function(){
                $('#form-fileupload_${elementParamName!}_${element.properties.elementUniqueKey!}').fileUploadField({
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

                // Handle dropdown toggle
                    $(document).on("click", ".menu-trigger", function(e) {
                        e.stopPropagation();
                        $(".menu-dropdown").hide(); // close others
                        $(this).siblings(".menu-dropdown").toggle();
                    });

                    // Show submenu on hover
                    $(document).on("mouseenter", ".has-submenu", function() {
                        $(this).children(".submenu").show();
                    }).on("mouseleave", ".has-submenu", function() {
                        $(this).children(".submenu").hide();
                    });

                    // Click outside to close
                    $(document).on("click", function() {
                        $(".menu-dropdown").hide();
                    });

                    // Remove file
                    $(document).on("click", ".remove-file", function() {
                        $(this).closest("li.file-item").remove();
                    });

                    // Edit actions
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

                        $(".menu-dropdown").hide(); // close menu after click
                    });

                    $(".file-item").each(function () {
                        const filename = $(this).data("filename");
                        if (!filename) return;

                        const lower = filename.toLowerCase();

                        // Show native edit ONLY for .docx
                        if (!lower.endsWith(".docx")) {
                            $(this).find(".open-native").remove();
                            $(this).find(".open-teams").remove();
                        }
                    });

            });
        </script>
    </#if>
</div>