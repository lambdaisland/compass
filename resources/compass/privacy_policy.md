# Confpass.me Privacy Policy

## 1. Who We Are and Our Role

Magpie Solutions BV (hereafter "we", "us", or "our") provides this conference and event scheduling platform as a hosted software service.

- The Data Controller: The company or organization organizing your specific event (e.g., your employer, the conference host) is the Data Controller. They decide *why* your data is collected and *what* it is used for.
- The Data Processor: Magpie Solutions BV acts as the Data Processor. We host and operate the software on behalf of the Controller. We do not own your data, nor do we use it for our own purposes.

## 2. What Data We Collect

To allow you to view the schedule and sign up for activities, the platform processes the following personal data:

- Discord user identifier ("snowflake") and Email address
- Profile information you provide
- User created content

*We do not collect special category data (e.g., health, religion, political views), payment details, or government IDs.*

## 3. Why We Process Your Data (Purpose)

- To authenticate your identity when logging in.
- To validate and link your conference ticket.
- To register you for chosen activities and manage capacity.

## 4. Where Your Data is Stored

All data is securely hosted within the European Union (Upcloud, Amsterdam). Your data is never transferred outside the EEA and Switzerland.

## 5. How Long We Keep Your Data (Retention)

We retain your data strictly for the duration of the event, plus a administrative period of 30 days following the event's conclusion.

After this 30-day window:

- All attendee personal data is automatically purged from the active database.
- All server backups and snapshots containing your data are manually deleted.

*If your specific event organizer requires a different retention period, they will notify you separately, but our platform will never retain your data longer than 30 days post-event without their explicit written instruction.*

## 6. Data Subject Rights (Access, Correction, Deletion)

Under GDPR, you have the right to access, correct, or delete your personal data.

Because we are the Processor, we cannot act on these requests without the Controller's authorization. Please direct all requests regarding your data to the organizer of your event (the company that invited you to this platform). If they instruct us to delete or export your data, we will comply promptly.

## 7. Data Security

We implement industry-standard security measures to protect your data, including:

- Encryption in transit: TLS 1.3 (HTTPS) for all web traffic.
- Access Controls: Strict server firewalls, MFA, process isolation (Defense-in-depth), and brute-force protection (*fail2ban*). Only the platform administrator (Magpie Solutions) has server access.

## 8. Sub-processors

We do not sell or share your data with third-party marketers. We use these sub-processors as part of our service:

### UpCloud Ltd.

**Service Provided**: Primary infrastructure provider, hosting core application and database server.

**Location of Processing**: Netherlands (NL-AMS1 Amsterdam data center)

**Personal Data Processed**: All personal data processed by the platform, including user account details (names, email addresses) and user-generated content, and other application data stored in databases and file systems. Server logs containing IP addresses are also processed.

**Security & Compliance**: ISO/IEC 27001:2022 certified. Data is encrypted at rest and in transit. A [Data Processing Agreement](https://upcloud.com/terms-of-service/#data-processing-agreement) (DPA) is in place governing this processing relationship. No transfer outside the EEA. All data remains within the European Union.

### Scaleway SAS

**Service Provided**: Failover infrastructure provider, hosting standby application and database server, to ensure high availability and business continuity in the event of primary infrastructure failure.

**Location of Processing**: France (Paris region).

**Personal Data Processed**: Replicated personal data from the primary infrastructure, including user account details (names, email addresses), user-generated content, and other application data stored in databases and file systems. Server logs containing IP addresses are also processed.

**Security & Compliance**: ISO/IEC 27001:2022 certified. Data is encrypted at rest and in transit. A [Data Processing Agreement](https://www-uploads.scaleway.com/DPA_2024_ENG_b0abb5cc26.pdf) (DPA) is in place governing this processing relationship. No transfer outside the EEA. All data remains within the European Union.

### Rsync.net

**Service Provided**: Offsite backup storage provider for application data and database dumps, enabling point-in-time recovery and data durability.

**Location of Processing**: Zurich, Switzerland.

**Personal Data Processed**: Encrypted backups of all application data, including user account details and, user-generated content. Because backups are stored using zero-knowledge encryption (data is encrypted client-side prior to upload), rsync.net cannot access, read, or decrypt the plaintext contents of any stored personal data.

**Legal Basis for Transfer**: Transfer to Switzerland, a third country with an EU adequacy decision under Article 45 of the GDPR. [A Data Processing Agreement](https://www.rsync.net/resources/regulatory/dpa.html) (DPA) is in place.

**Security & Compliance**: PCI DSS compliant. Zero-knowledge client-side encryption ensures that decryption keys are never shared with the provider. Data is stored immutably with versioning to protect against accidental deletion or ransomware. All data is encrypted at rest.

### Mux, Inc.

**Service Provided**: Video streaming infrastructure, including live stream delivery, video encoding, transcoding, and real-time analytics for viewer engagement and quality of experience.

**Location of Processing**: United States of America (and other locations where Mux maintains data processing operations).

**Personal Data Processed**: Limited technical data associated with viewer interactions, specifically:

- IP addresses (processed for coarse geolocation and bot detection; full IP address is truncated after processing in the EU before transfer to the US).
- Anonymized viewer identifiers (pseudonymous tokens that cannot be used by Mux to identify individual viewers).

**Legal Basis for Transfer**: EU-U.S. Data Privacy Framework (for transfers from the EEA to the US).

**Security & Compliance**: Mux holds ISO 27001 certification. A [Data Processing Agreement](https://www.mux.com/dpa) (DPA) is in place governing this sub-processing relationship.

## 9. Updates to this Policy

We may update this policy occasionally to reflect changes in software or regulations. The latest version will always be available via the link in the application footer.

## 10. Contact Us

If you have general questions about this platform's privacy practices (not about your specific event data), you can reach us at `contact` at `magpie.software`.

*For questions regarding your specific event data, please contact the event organizer directly.*
