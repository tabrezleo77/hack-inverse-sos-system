package com.brokencoders.narisuraksha.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brokencoders.narisuraksha.ui.theme.AppBackground
import com.brokencoders.narisuraksha.ui.theme.BorderDivider
import com.brokencoders.narisuraksha.ui.theme.ButtonCornerRadius
import com.brokencoders.narisuraksha.ui.theme.ButtonShape
import com.brokencoders.narisuraksha.ui.theme.CardBackground
import com.brokencoders.narisuraksha.ui.theme.CardCornerRadius
import com.brokencoders.narisuraksha.ui.theme.CardShape
import com.brokencoders.narisuraksha.ui.theme.DarkHeading
import com.brokencoders.narisuraksha.ui.theme.EmergencyRed
import com.brokencoders.narisuraksha.ui.theme.GuardianBlue
import com.brokencoders.narisuraksha.ui.theme.HeroGradientEnd
import com.brokencoders.narisuraksha.ui.theme.HeroGradientMiddle
import com.brokencoders.narisuraksha.ui.theme.HeroGradientStart
import com.brokencoders.narisuraksha.ui.theme.MagentaGradientEnd
import com.brokencoders.narisuraksha.ui.theme.MagentaGradientStart
import com.brokencoders.narisuraksha.ui.theme.NariSurakshaTheme
import com.brokencoders.narisuraksha.ui.theme.PillCornerRadius
import com.brokencoders.narisuraksha.ui.theme.PillShape
import com.brokencoders.narisuraksha.ui.theme.PrimaryPink
import com.brokencoders.narisuraksha.ui.theme.PrimaryPinkPressed
import com.brokencoders.narisuraksha.ui.theme.SafeGreen
import com.brokencoders.narisuraksha.ui.theme.SafeGreenSurface
import com.brokencoders.narisuraksha.ui.theme.SoftPinkSurface
import com.brokencoders.narisuraksha.ui.theme.SoftPinkSurfaceLight
import com.brokencoders.narisuraksha.ui.theme.SosGlow
import com.brokencoders.narisuraksha.ui.theme.TextPrimary
import com.brokencoders.narisuraksha.ui.theme.TextSecondary
import com.brokencoders.narisuraksha.ui.theme.WarningAmber

// =========================================================================
// 1. WomanShieldLogo
// Vector / Canvas reproduction of the Nari-Suraksha shield logo with
// the elegant silhouette of a woman inside (matching reference Image 1 & 2).
// =========================================================================
@Composable
fun WomanShieldLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = PrimaryPink
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.35f))
            .background(SoftPinkSurface),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.75f)) {
            val w = this.size.width
            val h = this.size.height

            // Shield outline path
            val shieldPath = Path().apply {
                moveTo(w * 0.5f, 0f)
                cubicTo(w * 0.85f, 0f, w, h * 0.2f, w, h * 0.45f)
                cubicTo(w, h * 0.75f, w * 0.5f, h, w * 0.5f, h)
                cubicTo(w * 0.5f, h, 0f, h * 0.75f, 0f, h * 0.45f)
                cubicTo(0f, h * 0.2f, w * 0.15f, 0f, w * 0.5f, 0f)
                close()
            }

            // Draw shield border
            drawPath(
                path = shieldPath,
                color = tint,
                style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
            )

            // Woman profile silhouette path
            val womanPath = Path().apply {
                // Forehead to nose and chin
                moveTo(w * 0.46f, h * 0.32f)
                cubicTo(w * 0.55f, h * 0.33f, w * 0.56f, h * 0.42f, w * 0.52f, h * 0.48f)
                // Chin to neck
                cubicTo(w * 0.48f, h * 0.53f, w * 0.49f, h * 0.62f, w * 0.45f, h * 0.68f)
                // Flowing hair swoop back
                cubicTo(w * 0.38f, h * 0.72f, w * 0.32f, h * 0.55f, w * 0.34f, h * 0.44f)
                cubicTo(w * 0.35f, h * 0.34f, w * 0.40f, h * 0.31f, w * 0.46f, h * 0.32f)
                close()
            }

            drawPath(path = womanPath, color = tint, style = Fill)

            // Little heart accent
            drawCircle(
                color = tint,
                radius = w * 0.07f,
                center = Offset(w * 0.62f, h * 0.36f)
            )
        }
    }
}

// =========================================================================
// 2. WomanSilhouetteArt
// Detailed illustrated woman profile silhouette with flowing hair,
// floral leaves, and heart accents (matching Image 1 & 2 hero banner).
// =========================================================================
@Composable
fun WomanSilhouetteArt(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // Outer soft shield / glow outline
            val shieldGlow = Path().apply {
                moveTo(w * 0.55f, h * 0.08f)
                cubicTo(w * 0.88f, h * 0.08f, w * 0.98f, h * 0.30f, w * 0.95f, h * 0.55f)
                cubicTo(w * 0.90f, h * 0.82f, w * 0.55f, h * 0.98f, w * 0.55f, h * 0.98f)
                cubicTo(w * 0.55f, h * 0.98f, w * 0.20f, h * 0.82f, w * 0.15f, h * 0.55f)
                cubicTo(w * 0.12f, h * 0.30f, w * 0.22f, h * 0.08f, w * 0.55f, h * 0.08f)
                close()
            }
            drawPath(
                path = shieldGlow,
                color = SoftPinkSurface,
                style = Stroke(width = w * 0.04f)
            )

            // Flowing Hair Main Sweep (Deep Magenta)
            val hairPath = Path().apply {
                moveTo(w * 0.48f, h * 0.16f)
                cubicTo(w * 0.68f, h * 0.18f, w * 0.82f, h * 0.32f, w * 0.78f, h * 0.56f)
                cubicTo(w * 0.75f, h * 0.78f, w * 0.58f, h * 0.92f, w * 0.42f, h * 0.92f)
                cubicTo(w * 0.35f, h * 0.82f, w * 0.44f, h * 0.72f, w * 0.52f, h * 0.64f)
                cubicTo(w * 0.62f, h * 0.52f, w * 0.60f, h * 0.38f, w * 0.48f, h * 0.28f)
                close()
            }
            drawPath(path = hairPath, color = PrimaryPink)

            // Elegant Face Profile
            val facePath = Path().apply {
                moveTo(w * 0.48f, h * 0.28f)
                // Forehead to nose tip
                lineTo(w * 0.44f, h * 0.38f)
                lineTo(w * 0.41f, h * 0.41f)
                // Nose to lips
                lineTo(w * 0.43f, h * 0.44f)
                // Lips to chin
                lineTo(w * 0.42f, h * 0.48f)
                lineTo(w * 0.44f, h * 0.52f)
                // Jawline to neck
                cubicTo(w * 0.48f, h * 0.56f, w * 0.50f, h * 0.64f, w * 0.52f, h * 0.70f)
                // Neck back to hairline
                cubicTo(w * 0.56f, h * 0.60f, w * 0.58f, h * 0.44f, w * 0.48f, h * 0.28f)
                close()
            }
            drawPath(path = facePath, color = PrimaryPinkPressed)

            // Floating heart accents
            drawCircle(color = PrimaryPink, radius = w * 0.035f, center = Offset(w * 0.86f, h * 0.32f))
            drawCircle(color = PrimaryPink, radius = w * 0.025f, center = Offset(w * 0.84f, h * 0.44f))

            // Gentle leaves accent on lower left
            val leafPath = Path().apply {
                moveTo(w * 0.22f, h * 0.68f)
                cubicTo(w * 0.15f, h * 0.60f, w * 0.24f, h * 0.54f, w * 0.32f, h * 0.60f)
                cubicTo(w * 0.38f, h * 0.68f, w * 0.30f, h * 0.74f, w * 0.22f, h * 0.68f)
                close()
            }
            drawPath(path = leafPath, color = SoftPinkSurface)
        }
    }
}

// =========================================================================
// 3. HeroBannerCard
// Large pink gradient hero banner matching Image 1 & 2:
// "Your Safety, Our Priority", subtitle, pill button "(SOS) Tap for Immediate Help ->",
// and woman silhouette illustration with calligraphy text.
// =========================================================================
@Composable
fun HeroBannerCard(
    onImmediateHelpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = CardShape, spotColor = PrimaryPink.copy(alpha = 0.15f)),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(HeroGradientStart, HeroGradientMiddle, HeroGradientEnd)
                    )
                )
                .border(BorderStroke(1.5.dp, BorderDivider), CardShape)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left text & actions column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    // Category Chip
                    Row(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(Color.White.copy(alpha = 0.85f))
                            .border(1.dp, BorderDivider, PillShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = PrimaryPink,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Women's Safety",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = PrimaryPink
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hero Heading: 34sp Bold
                    Text(
                        text = "Your Safety\nOur Priority ♥",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp,
                            lineHeight = 36.sp
                        ),
                        color = DarkHeading
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Subtitle: 14sp
                    Text(
                        text = "Stay safe. Stay connected.\nWe are always with you.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 19.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Pill Button: (SOS) Tap for Immediate Help ->
                    Button(
                        onClick = onImmediateHelpClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryPink,
                            contentColor = Color.White
                        ),
                        shape = PillShape,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SOS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                ),
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tap for Immediate Help",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    }
                }

                // Right illustration column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    WomanSilhouetteArt(size = 130.dp)
                    Text(
                        text = "Safety Builds Confidence",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        color = PrimaryPink
                    )
                }
            }
        }
    }
}

// =========================================================================
// 4. QuickActionCard
// Matching the 4 quick action cards from Image 2:
// - Emergency (SOS badge)
// - Share Location (Location pin)
// - Safety Mesh (Shield)
// - Quick Record (Video / Camcorder)
// =========================================================================
@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .shadow(elevation = 2.dp, shape = CardShape, spotColor = PrimaryPink.copy(alpha = 0.10f)),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, BorderDivider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Circular Magenta Icon Container (48dp)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(MagentaGradientStart, MagentaGradientEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (badgeText != null) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            ),
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Arrow Circle Button (->)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(SoftPinkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open $title",
                        tint = PrimaryPink,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card Title (16sp semibold)
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = DarkHeading
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Card Subtitle (12-14sp)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                ),
                color = TextSecondary,
                maxLines = 2
            )
        }
    }
}

// =========================================================================
// 5. SafetyStatusCard
// Matching the "Safety Status" card in Image 2:
// Green status dot ("You are Safe", "Your location is not being tracked."),
// last updated time, and "Share Location" action button.
// =========================================================================
@Composable
fun SafetyStatusCard(
    isSafe: Boolean,
    lastUpdatedText: String,
    onShareLocationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = CardShape, spotColor = PrimaryPink.copy(alpha = 0.08f)),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, BorderDivider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Status row inside soft pink container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSafe) SoftPinkSurfaceLight else EmergencyRed.copy(alpha = 0.1f))
                    .border(1.dp, if (isSafe) BorderDivider else EmergencyRed.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Green pulsing status indicator
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isSafe) SafeGreen else EmergencyRed)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isSafe) "You are Safe" else "Distress Broadcast Active",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = if (isSafe) DarkHeading else EmergencyRed
                            )
                            Text(
                                text = if (isSafe) "Your location is not being tracked." else "Mesh relay broadcasting SOS packet.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = TextSecondary
                            )
                        }
                    }

                    // Shield check icon
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isSafe) SafeGreenSurface else EmergencyRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isSafe) SafeGreen else EmergencyRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer row: Last Updated + Share Location button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = PrimaryPink,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Last Updated",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextSecondary
                        )
                        Text(
                            text = lastUpdatedText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = DarkHeading
                        )
                    }
                }

                // Share Location pill button
                OutlinedButton(
                    onClick = onShareLocationClick,
                    shape = PillShape,
                    border = BorderStroke(1.dp, PrimaryPink),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = PrimaryPink
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Share Location",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

// =========================================================================
// 6. LatestUpdatesCard
// Matching the "Latest Updates" section in Image 2:
// - Safety Tips for Women
// - Community Support
// - Emergency Contacts
// =========================================================================
@Composable
fun LatestUpdatesCard(
    onTipsClick: () -> Unit,
    onCommunityClick: () -> Unit,
    onContactsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = CardShape, spotColor = PrimaryPink.copy(alpha = 0.08f)),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, BorderDivider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            UpdateRowItem(
                icon = Icons.Default.Campaign,
                title = "Safety Tips for Women",
                subtitle = "Simple steps for everyday safety.",
                onClick = onTipsClick
            )

            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderDivider.copy(alpha = 0.5f)))
            Spacer(modifier = Modifier.height(10.dp))

            UpdateRowItem(
                icon = Icons.Default.People,
                title = "Community Support",
                subtitle = "Find help and connect with others.",
                onClick = onCommunityClick
            )

            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderDivider.copy(alpha = 0.5f)))
            Spacer(modifier = Modifier.height(10.dp))

            UpdateRowItem(
                icon = Icons.Default.Contacts,
                title = "Emergency Contacts",
                subtitle = "Police, Helpline and more.",
                onClick = onContactsClick
            )
        }
    }
}

@Composable
private fun UpdateRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SoftPinkSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryPink,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = DarkHeading
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = TextSecondary
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open $title",
            tint = PrimaryPink,
            modifier = Modifier.size(20.dp)
        )
    }
}

// =========================================================================
// 7. StaySafeBanner
// Matching Image 1 & 2 bottom banner:
// "You are not alone. Help is just a tap away." | [Heart] Stay Safe
// =========================================================================
@Composable
fun StaySafeBanner(
    onStaySafeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SoftPinkSurface)
            .border(1.dp, BorderDivider, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                WomanShieldLogo(size = 32.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "You are not alone. Help is just a tap away.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = DarkHeading
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onStaySafeClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                shape = PillShape,
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Stay Safe",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}

// =========================================================================
// 8. AppBottomBar
// Bottom navigation bar matching Image 2:
// Home, Safety, prominent central SOS button (elevated circular magenta),
// Community, and Profile!
// =========================================================================
@Composable
fun AppBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSosClick: () -> Unit = { onNavigate("home") }
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
    ) {
        // Bottom bar surface (72dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .align(Alignment.BottomCenter),
            color = CardBackground,
            border = BorderStroke(1.dp, BorderDivider),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Home
                BottomBarItem(
                    label = "Home",
                    icon = Icons.Default.Home,
                    isSelected = currentRoute == "home",
                    onClick = { onNavigate("home") }
                )

                // 2. Safety
                BottomBarItem(
                    label = "Safety",
                    icon = Icons.Default.Shield,
                    isSelected = currentRoute == "safety_alerts" || currentRoute == "safety" || currentRoute == "safe_zones",
                    onClick = { onNavigate("safety_alerts") }
                )

                // Placeholder space for the central elevated SOS button
                Spacer(modifier = Modifier.width(64.dp))

                // 4. Community
                BottomBarItem(
                    label = "Community",
                    icon = Icons.Default.People,
                    isSelected = currentRoute == "community",
                    onClick = { onNavigate("community") }
                )

                // 5. Profile
                BottomBarItem(
                    label = "Profile",
                    icon = Icons.Default.Person,
                    isSelected = currentRoute == "profile" || currentRoute == "settings",
                    onClick = { onNavigate("profile") }
                )
            }
        }

        // Central Prominent Glowing SOS Button (elevated above the bar)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-4).dp)
        ) {
            // Subtle glowing ring
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(SosGlow)
                    .align(Alignment.Center)
            )

            // Central elevated circular button
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.Center)
                    .shadow(elevation = 6.dp, shape = CircleShape, spotColor = PrimaryPink)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(MagentaGradientStart, MagentaGradientEnd)
                        )
                    )
                    .border(2.5.dp, Color.White, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = Color.White),
                        onClick = onSosClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SOS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun RowScope.BottomBarItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = PrimaryPink
    val inactiveColor = TextSecondary

    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) SoftPinkSurface else Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) activeColor else inactiveColor
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
            ),
            color = if (isSelected) activeColor else inactiveColor
        )
    }
}

// =========================================================================
// 9. Standard Reusable Components:
// PrimaryButton, SecondaryButton, DangerButton, StatusChip, InfoCard, etc.
// =========================================================================
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 56.dp
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(elevation = 3.dp, shape = ButtonShape, spotColor = PrimaryPink.copy(alpha = 0.25f)),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryPink,
            contentColor = Color.White,
            disabledContainerColor = PrimaryPink.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
        }
    }
}

@Composable
fun PrimaryAppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 56.dp
) = PrimaryButton(text = text, onClick = onClick, modifier = modifier, icon = icon, enabled = enabled, height = height)

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 56.dp
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = ButtonShape,
        border = BorderStroke(1.5.dp, PrimaryPink),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = PrimaryPink
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = PrimaryPink)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = PrimaryPink
            )
        }
    }
}

@Composable
fun SecondaryAppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 56.dp
) = SecondaryButton(text = text, onClick = onClick, modifier = modifier, icon = icon, enabled = enabled, height = height)

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = CardBackground,
    borderColor: Color = BorderDivider,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = EmergencyRed,
            contentColor = Color.White
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = Color.White
            )
        }
    }
}

@Composable
fun StatusChip(
    label: String,
    icon: ImageVector,
    statusColor: Color,
    modifier: Modifier = Modifier,
    containerColor: Color = SoftPinkSurface
) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(containerColor)
            .border(1.dp, BorderDivider, PillShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            ),
            color = TextPrimary
        )
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = CardBackground,
    borderColor: Color = BorderDivider,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = CardShape, spotColor = PrimaryPink.copy(alpha = 0.08f)),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(modifier = Modifier.padding(18.dp)) {
            content()
        }
    }
}

@Composable
fun CounterTile(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color = PrimaryPink,
    isDemoScale: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(elevation = 1.dp, shape = CardShape, spotColor = color.copy(alpha = 0.1f)),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, BorderDivider)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = DarkHeading
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = TextSecondary
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = DarkHeading
        )
        if (actionText != null && onActionClick != null) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onActionClick)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = PrimaryPink
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = PrimaryPink,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    headline: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SoftPinkSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryPink, modifier = Modifier.size(38.dp))
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = headline,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = DarkHeading,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(18.dp))
            action()
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Cancel",
    isDanger: Boolean = false,
    icon: ImageVector? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDanger) EmergencyRed else PrimaryPink,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else null,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = DarkHeading
            )
        },
        text = {
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = if (isDanger) EmergencyRed else PrimaryPink),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(confirmText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText, color = TextSecondary)
            }
        },
        shape = CardShape,
        containerColor = CardBackground
    )
}

@Composable
fun MeshStatusBadge(
    isMeshActive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(if (isMeshActive) SafeGreenSurface else SoftPinkSurface)
            .border(1.dp, if (isMeshActive) SafeGreen else BorderDivider, PillShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isMeshActive) SafeGreen else PrimaryPink)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isMeshActive) "Mesh Active" else "Mesh Offline",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
            color = if (isMeshActive) SafeGreen else PrimaryPink
        )
    }
}

@Composable
fun SosButton(
    isBroadcasting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = if (isBroadcasting) "BROADCASTING SOS" else "HOLD FOR HELP"
) {
    val heroColor = if (isBroadcasting) EmergencyRed else PrimaryPink

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(heroColor.copy(alpha = 0.15f))
        )

        // Hero Button
        Box(
            modifier = Modifier
                .size(160.dp)
                .shadow(elevation = 8.dp, shape = CircleShape, spotColor = heroColor.copy(alpha = 0.35f))
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(heroColor, PrimaryPinkPressed)
                    )
                )
                .border(3.dp, Color.White, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, radius = 80.dp, color = Color.White),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isBroadcasting) Icons.Default.Warning else Icons.Default.Shield,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
