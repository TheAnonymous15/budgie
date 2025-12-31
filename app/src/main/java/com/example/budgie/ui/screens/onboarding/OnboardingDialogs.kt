package com.example.budgie.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * "Why do I need your birthday?" helper item
 */
@Composable
fun WhyBenefitItem(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    Color(0xFF10B981).copy(alpha = 0.1f),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

/**
 * "Why do I need your birthday?" dialog
 */
@Composable
fun WhyBirthdayDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B263B),
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Color(0xFF10B981).copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Text(
                text = "Personalizing Your Experience",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "A little information helps Budgie deliver smarter and more relevant financial guidance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    thickness = 1.dp
                )

                WhyBenefitItem(
                    icon = Icons.Filled.Person,
                    text = "Insights personalized to your financial profile"
                )

                WhyBenefitItem(
                    icon = Icons.Filled.Cake,
                    text = "Helpful birthday reminders and milestones"
                )

                WhyBenefitItem(
                    icon = Icons.Filled.Psychology,
                    text = "Advice aligned with your life stage"
                )

                WhyBenefitItem(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    text = "Tailored wealth-building strategies"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFF10B981).copy(alpha = 0.08f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Accurate details lead to better financial insights.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Continue",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

/**
 * Underage (below 18) warning dialog
 */
@Composable
fun UnderageDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Age Requirement Not Met",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "You must be at least 18 years old to use Budgie.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Budgie is a financial management app designed for adults who can make independent financial decisions.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "In the meantime, consider learning about personal finance through age-appropriate resources!",
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "I Understand",
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * Terms of Use Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfUseDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Terms of Use",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A472A)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF666666)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Last Updated: December 25, 2025",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    item {
                        TermsInfoCard(
                            text = "Please read these terms carefully before using Budgie. By clicking 'Start Building Wealth!' you acknowledge that you have read, understood, and agree to be bound by these Terms of Use."
                        )
                    }

                    item { TermsSection("1. Acceptance of Terms", TERMS_ACCEPTANCE) }
                    item { TermsSection("2. Eligibility and Age Requirement", TERMS_ELIGIBILITY) }
                    item { TermsSection("3. License to Use", TERMS_LICENSE) }
                    item { TermsSection("4. Acceptable Use Policy", TERMS_ACCEPTABLE_USE) }
                    item { TermsSection("5. User Data and Privacy", TERMS_USER_DATA) }
                    item { TermsSection("6. Financial Information Disclaimer", TERMS_FINANCIAL_DISCLAIMER) }
                    item { TermsSection("7. Accuracy of Information", TERMS_ACCURACY) }
                    item { TermsSection("8. Intellectual Property Rights", TERMS_IP) }
                    item { TermsSection("9. Limitation of Liability", TERMS_LIABILITY) }
                    item { TermsSection("10. Indemnification", TERMS_INDEMNIFICATION) }
                    item { TermsSection("11. Termination", TERMS_TERMINATION) }
                    item { TermsSection("12. Changes to Terms", TERMS_CHANGES) }
                    item { TermsSection("13. Governing Law", TERMS_GOVERNING_LAW) }
                    item { TermsSection("14. Severability", TERMS_SEVERABILITY) }
                    item { TermsSection("15. Contact Information", TERMS_CONTACT) }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("I Understand", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Privacy Policy Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A1929)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF666666)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Last Updated: December 25, 2025",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    item {
                        PrivacyHighlightCard()
                    }

                    item { TermsSection("1. Information We Collect", PRIVACY_INFO_COLLECT) }
                    item { TermsSection("2. How We Use Your Birthday", PRIVACY_BIRTHDAY_USE) }
                    item { TermsSection("3. Data Storage & Security", PRIVACY_DATA_STORAGE) }
                    item { TermsSection("4. Data Sharing & Third Parties", PRIVACY_DATA_SHARING) }
                    item { TermsSection("5. Cookies & Tracking", PRIVACY_COOKIES) }
                    item { TermsSection("6. Your Rights & Control", PRIVACY_YOUR_RIGHTS) }
                    item { TermsSection("7. Children's Privacy", PRIVACY_CHILDREN) }
                    item { TermsSection("8. Data Retention", PRIVACY_RETENTION) }
                    item { TermsSection("9. International Data Transfers", PRIVACY_INTERNATIONAL) }
                    item { TermsSection("10. Changes to This Privacy Policy", PRIVACY_CHANGES) }
                    item { TermsSection("11. Legal Compliance", PRIVACY_LEGAL) }
                    item { TermsSection("12. Contact Us", PRIVACY_CONTACT) }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("I Understand", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TermsInfoCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE0F2FE)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0A1929)
            )
        }
    }
}

@Composable
private fun PrivacyHighlightCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE0F2FE)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "Your Privacy, Our Promise",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0A1929)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Budgie ONLY collects your date of birth. All other data stays exclusively on your device. We don't collect, transmit, or sell your information.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E3A8A)
                )
            }
        }
    }
}

@Composable
private fun TermsSection(title: String, content: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0A1929)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF333333)
        )
    }
}

// Content strings for Terms
private const val TERMS_ACCEPTANCE = "By accessing or using Budgie (\"the App\"), you agree to be legally bound by these Terms of Use. These terms constitute a binding legal agreement between you and Budgie. If you do not agree to these terms in their entirety, you must not use the App.\n\nYour continued use of the App following any amendments to these Terms will constitute your acceptance of such amendments."

private const val TERMS_ELIGIBILITY = "You must be at least 18 years of age to use Budgie. By using this App, you represent and warrant that:\n\n• You are 18 years of age or older\n• You have the legal capacity to enter into a binding agreement\n• You are using the App for personal, non-commercial purposes\n• All information you provide is accurate and truthful\n\nWe reserve the right to request proof of age and terminate accounts that do not meet this requirement."

private const val TERMS_LICENSE = "Subject to your compliance with these Terms, we grant you a limited, non-exclusive, non-transferable, revocable license to:\n\n• Download and install the App on your personal device\n• Access and use the App for personal financial management\n\nThis license does NOT permit you to:\n\n• Modify, copy, or distribute the App\n• Reverse engineer, decompile, or disassemble the App\n• Create derivative works based on the App\n• Use the App for commercial purposes\n• Remove or alter any copyright notices"

private const val TERMS_ACCEPTABLE_USE = "You agree to use Budgie only for lawful purposes and in accordance with these Terms. You agree NOT to:\n\n• Violate any local, state, national, or international law\n• Interfere with or disrupt the App's functionality\n• Attempt to gain unauthorized access to the App\n• Use automated systems to access the App\n• Introduce viruses, malware, or harmful code\n• Impersonate any person or entity\n• Use the App to transmit false or misleading information"

private const val TERMS_USER_DATA = "Data Collection: Budgie only collects your date of birth, which is stored locally on your device. No other personal data is collected or transmitted by Budgie.\n\nData Storage: All your financial data (expenses, income, bills, budgets) is stored exclusively on your device using local database storage.\n\nNo Cloud Storage: We do NOT sync your data to cloud servers. We do NOT have servers that receive or store your data.\n\nData Transmission - YOU ARE IN CONTROL:\n• Budgie NEVER automatically transmits your data anywhere\n• ONLY YOU can choose to export or share your data\n• When you export reports (PDF, Excel), YOU control where they go\n• You decide who receives your exported reports\n• We have ZERO access to your exported files\n\nPlease read our Privacy Policy for detailed information about data handling."

private const val TERMS_FINANCIAL_DISCLAIMER = "IMPORTANT: Budgie provides general financial insights and suggestions based on your data. These are NOT professional financial advice.\n\n• The App's insights are algorithmic and automated\n• They do NOT constitute professional financial, investment, legal, or tax advice\n• You should NOT make financial decisions based solely on the App's suggestions\n• Always consult with qualified financial advisors before making significant financial decisions\n\nWe are NOT licensed financial advisors and do NOT provide personalized financial advice tailored to your specific circumstances."

private const val TERMS_ACCURACY = "You acknowledge that:\n\n• You are responsible for the accuracy of all data you enter\n• The App's calculations are only as accurate as the data you provide\n• We do NOT verify or validate your financial data\n• Errors in data entry may lead to incorrect insights\n• You should regularly review and verify your financial information"

private const val TERMS_IP = "All intellectual property rights in and to the App, including but not limited to:\n\n• Software code and algorithms\n• Design, graphics, and user interface\n• Text, images, and content\n• Trademarks and branding\n\nare owned by or licensed to Budgie. You acknowledge that you have no rights to the App except for the limited license granted in these Terms."

private const val TERMS_LIABILITY = "TO THE MAXIMUM EXTENT PERMITTED BY LAW:\n\n• The App is provided \"AS IS\" and \"AS AVAILABLE\"\n• We make NO warranties, express or implied\n• We are NOT liable for any direct, indirect, incidental, special, consequential, or punitive damages\n• This includes but is not limited to: financial losses, data loss, loss of profits, business interruption\n• Our total liability shall not exceed KSh 1,000\n\nSome jurisdictions do not allow limitations on liability, so these limitations may not apply to you."

private const val TERMS_INDEMNIFICATION = "You agree to indemnify, defend, and hold harmless Budgie, its developers, and affiliates from any claims, damages, losses, liabilities, and expenses (including legal fees) arising from:\n\n• Your use or misuse of the App\n• Your violation of these Terms\n• Your violation of any rights of another party\n• Any financial decisions you make based on App insights"

private const val TERMS_TERMINATION = "We reserve the right to:\n\n• Suspend or terminate your access at any time\n• Discontinue the App without notice\n• Modify or remove features\n\nYou may terminate your use by uninstalling the App. Upon termination, all licenses granted to you will cease immediately."

private const val TERMS_CHANGES = "We reserve the right to modify these Terms at any time. When we make changes:\n\n• We will update the \"Last Updated\" date\n• Significant changes will be communicated via the App\n• Your continued use constitutes acceptance of the new Terms\n\nIf you do not agree to the modified Terms, you must stop using the App."

private const val TERMS_GOVERNING_LAW = "These Terms shall be governed by and construed in accordance with the laws of Kenya, without regard to its conflict of law provisions. Any disputes shall be resolved in the courts of Kenya."

private const val TERMS_SEVERABILITY = "If any provision of these Terms is found to be unenforceable or invalid, that provision shall be limited or eliminated to the minimum extent necessary, and the remaining provisions shall remain in full force and effect."

private const val TERMS_CONTACT = "For questions, concerns, or notices regarding these Terms of Use:\n\nEmail: legal@budgieapp.com\nSupport: support@budgieapp.com\n\nWe aim to respond to all inquiries within 48 hours."

// Content strings for Privacy Policy
private const val PRIVACY_INFO_COLLECT = "Personal Data We Collect:\n\nBudgie collects ONLY ONE piece of personal data:\n\n• Your Date of Birth (birthday)\n\nPurpose of Collection:\n• Age verification (18+ requirement)\n• Personalized birthday greetings\n• Age-appropriate financial insights\n• Life-stage specific recommendations\n\nData You Store Locally:\n\nThe following data is stored ONLY on your device and is NOT collected by us:\n\n• Your name/nickname\n• Financial transactions (expenses, income)\n• Bills and budgets\n• Any notes or categories you create\n\nWe have NO access to this information."

private const val PRIVACY_BIRTHDAY_USE = "Your date of birth is used exclusively for:\n\n1. Age Verification\n   • Ensuring you meet the 18+ requirement\n   • Complying with legal age restrictions\n\n2. Personalization\n   • Displaying birthday wishes on your special day\n   • Personalizing greeting messages\n\n3. Financial Insights\n   • Calculating your age for life-stage analysis\n   • Providing age-appropriate financial recommendations\n   • Retirement planning calculations\n   • Long-term wealth projections\n\n4. Analytics (Local Only)\n   • Understanding user age demographics\n   • Improving app features for different age groups\n\nAll processing happens locally on your device. Your birthday is NEVER transmitted to our servers or any third party."

private const val PRIVACY_DATA_STORAGE = "Storage Location:\n\n• All data is stored in your device's local database using Android's secure storage\n• Data is encrypted using your device's built-in security\n• We do NOT use cloud storage\n• We do NOT sync data across devices\n\nData Transmission - YOU ARE IN COMPLETE CONTROL:\n\n• Budgie NEVER transmits your data automatically\n• We do NOT have backend servers collecting user data\n• The app functions entirely offline after installation\n• No internet connection is required for core functionality\n\nONLY YOU Control Data Exports:\n• When you export reports (PDF, Excel), YOU initiate the action\n• YOU choose where to save or send exported files\n• YOU decide who receives your financial reports\n• Budgie has NO access to your exported files once created"

private const val PRIVACY_DATA_SHARING = "WE DO NOT SHARE YOUR DATA. PERIOD.\n\n• We do NOT sell your data to advertisers\n• We do NOT share data with marketing companies\n• We do NOT provide data to data brokers\n• We do NOT use analytics services that collect personal data\n• We do NOT have partnerships that involve data sharing\n\nThere are NO third parties involved in Budgie's operation."

private const val PRIVACY_COOKIES = "Budgie does NOT use:\n\n• Cookies\n• Web beacons\n• Tracking pixels\n• Analytics SDKs\n• Advertising identifiers\n• Session tracking\n\nWe do NOT track your behavior, location, or device information."

private const val PRIVACY_YOUR_RIGHTS = "You have COMPLETE control over your data:\n\n1. Right to Access\n   • View your birthday anytime in the app\n   • Access all financial data you've entered\n\n2. Right to Modify\n   • Change your personal information anytime\n   • Edit or delete any financial records\n\n3. Right to Delete\n   • Uninstall the app to permanently delete all data\n   • Clear app data from device settings\n   • No residual data remains after deletion\n\n4. Right to Export - YOUR DATA, YOUR CHOICE\n   • Export your data to PDF or Excel anytime\n   • YOU initiate all exports - we never do\n   • Choose exactly what data to include\n   • Take your data with you - it's YOURS"

private const val PRIVACY_CHILDREN = "Budgie is NOT intended for individuals under 18 years of age.\n\n• We do NOT knowingly collect data from minors\n• Age verification is required during onboarding\n• Users under 18 cannot proceed past the onboarding screen\n\nParents: If you believe your child has used Budgie, please contact us immediately."

private const val PRIVACY_RETENTION = "Your data is retained:\n\n• Only on your device\n• For as long as you keep the app installed\n• Until you choose to delete it\n\nWe do NOT retain any data because we don't collect it in the first place!"

private const val PRIVACY_INTERNATIONAL = "Since all data is stored locally on your device:\n\n• There are NO international data transfers\n• Your data never leaves your device\n• GDPR, CCPA, and other privacy regulations don't apply in the traditional sense\n\nYour data stays in your country, on your device, in your control."

private const val PRIVACY_CHANGES = "We may update this Privacy Policy from time to time. When we do:\n\n• We will update the \"Last Updated\" date\n• Material changes will be communicated via the app\n• You will be notified of significant changes\n\nContinued use after changes constitutes acceptance of the updated policy."

private const val PRIVACY_LEGAL = "We comply with:\n\n• Kenya Data Protection Act, 2019\n• General Data Protection Regulation (GDPR) principles\n• California Consumer Privacy Act (CCPA) where applicable\n\nHowever, since we only collect your birthday and store everything locally, most regulations don't apply in the traditional sense."

private const val PRIVACY_CONTACT = "If you have questions, concerns, or requests regarding this Privacy Policy or your data:\n\nPrivacy Officer: privacy@budgieapp.com\nGeneral Support: support@budgieapp.com\nLegal Inquiries: legal@budgieapp.com\n\nWe aim to respond within 48 hours.\n\nMailing Address:\nBudgie App\nNairobi, Kenya\n\nWe're committed to protecting your privacy and will address any concerns promptly."

