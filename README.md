# SharePoint File Upload Plugin (Joget)

## Introduction

The **SharePoint File Upload Plugin** enables Joget applications to upload and manage documents directly in **Microsoft SharePoint Online** instead of storing files locally or in the Joget database.

This plugin integrates with **Microsoft Graph API** and supports secure document storage, metadata synchronization, and multiple document actions while keeping Joget as the system of record.

---

## Key Features

* Upload files from Joget forms directly to SharePoint
* Support for dynamic folder paths using Joget variables
* Store and map SharePoint file metadata into Joget forms
* Download documents from SharePoint
* Update or replace existing documents
* Delete documents from SharePoint
* Open documents in SharePoint Web, Microsoft Teams, or local desktop apps
* Secure authentication using Azure AD credentials

---

## Prerequisites

* Joget **Enterprise Edition**
* Developer access to Joget App Center
* Azure AD App Registration with Microsoft Graph permissions
* SharePoint Online Site and Document Library
* Tenant ID, Client ID, Client Secret, Site ID, and Drive ID

---

## Installation

1. Log in to **Joget App Center**.
2. Navigate to **Settings → Manage Plugins**.
3. Click **Upload Plugin**.
4. Upload the **SharePoint File Upload Plugin JAR** file.
5. Confirm the plugin appears under **Installed Plugins**.

---

## Usage

### Add Plugin to Form

1. Open **Form Builder** in your Joget app.
2. Drag **File Upload to External Storage** from **Mokxa Plugins** into the form.
3. Open the element configuration.

### Basic Configuration

* **Label** – Display name shown to users
* **ID** – Internal form field identifier

### Advanced Options

(Standard Joget file upload settings)

* Maximum file size
* Allowed file types
* Mandatory validation
* Permission control

---

## SharePoint Configuration

Configure the following fields (recommended via App Variables):

* **Tenant ID**: `#appVariable.tenantId#`
* **Client ID**: `#appVariable.clientId#`
* **Client Secret**: `#appVariable.clientSecret#`
* **Site ID**: `#appVariable.siteId#`
* **Drive ID**: `#appVariable.driveId#`
* **List ID (Optional)**: `#appVariable.listId#`
* **Upload Folder Path**: e.g. `case/{id}`

---

## Metadata Mapping

The plugin supports saving SharePoint file metadata into a Joget form.

Supported metadata includes:

* File ID
* File Name
* Version
* Document / Content Type
* Description
* Created Date Time
* Last Modified Date Time
* Uploaded By
* File Size

Each SharePoint metadata field can be mapped to a corresponding Joget form field.

---

## Supported Actions

* **Upload** – Upload new files to SharePoint (auto-create folders)
* **Download** – Download files from SharePoint
* **Update / Replace** – Replace existing files while retaining metadata
* **Delete** – Remove files from SharePoint
* **Open / Edit** – Open files in SharePoint Web, Teams, or local applications

---

## Best Practices

* Store credentials securely using **App Variables**
* Use structured folder paths for better document organization
* Enable metadata storage for auditing and reporting
* Apply role-based permissions in Joget forms

---

## License & Support

This plugin is intended for enterprise Joget deployments.
For support, enhancements, or issues, please contact the plugin provider or refer to the project repository.

---

## Conclusion

The SharePoint File Upload Plugin provides a robust, secure, and enterprise-ready solution for managing documents in SharePoint while seamlessly integrating with Joget workflows and forms.
