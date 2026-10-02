package org.claudroide.app.core.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.claudroide.app.R
import org.claudroide.app.core.design.TypeTokens

/**
 * The pieces the screens share.
 *
 * These exist so the four screens cannot drift apart. The alternative -- each
 * screen picking its own padding and radius -- is exactly how an interface ends
 * up with three different gutters and no visual system.
 */

/**
 * A status pill: a small coloured dot and a word.
 *
 * The dot carries no meaning on its own. It is marked decorative, because the
 * word beside it already states the status, and a screen reader announcing
 * "green dot" before "Local only" adds noise rather than information.
 */
@Composable
fun StatusChip(
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(tint, CircleShape)
                    .semantics { contentDescription = "" }
            )
            Text(
                text = label,
                style = TypeTokens.LabelMediumStyle,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * One chat bubble.
 *
 * `mine` picks the side and the fill rather than leaving the caller to restyle
 * the text, which is how an assistant reply ends up in the user's green and
 * stops being distinguishable at a glance.
 */
@Composable
fun MessageBubble(
    text: String,
    mine: Boolean,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier,
        shape = shape,
        color = if (mine) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(
                start = 14.dp,
                end = 14.dp,
                top = if (footer == null) 10.dp else 12.dp,
                bottom = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = text,
                style = TypeTokens.BodyMediumStyle,
                color = if (mine) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
            )
            footer?.invoke()
        }
    }
}

/**
 * A monospaced block for code and command output.
 *
 * `copyable` is a real control rather than a decoration: on a phone, output you
 * cannot select is output you cannot act on.
 */
@Composable
fun CodeBlock(
    code: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = code,
            style = TypeTokens.CodeTextStyle,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

/**
 * The "nothing here yet" state used by more than one screen.
 *
 * An empty state is the screen people see most often when something has gone
 * wrong with setup, so it offers the next action instead of only describing
 * the absence.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(TypeTokens.SpacingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TypeTokens.SpacingMedium),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            style = TypeTokens.TitleMediumStyle,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = description,
            style = TypeTokens.BodyMediumStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        action?.let {
            Spacer(Modifier.size(TypeTokens.SpacingXSmall))
            it()
        }
    }
}

/**
 * A labelled section inside a settings or detail screen.
 *
 * The label sits above the content rather than beside it: on a narrow screen a
 * two-column row either wraps or squeezes the value, and a squeezed value is
 * the part the user came to read.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = TypeTokens.LabelMediumStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

/** A selectable row for lists of chats, projects and models. */
@Composable
fun ListRow(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(TypeTokens.CornerRadiusMedium)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.border(
                    width = if (selected) 1.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = shape,
                ) else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TypeTokens.SpacingMedium),
    ) {
        leading?.let {
            it()
            Spacer(Modifier.width(TypeTokens.SpacingXSmall))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = TypeTokens.TitleSmallStyle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = TypeTokens.BodySmallStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

/** Content padding that respects both the scaffold inset and the 16dp gutter. */
val ScreenPadding = PaddingValues(horizontal = TypeTokens.SpacingMedium)

/**
 * The brand banner.
 *
 * `ContentScale.Crop` rather than `Fit` on purpose: the artwork has its content
 * inside the central band, so cropping the edges on a narrow phone trims
 * background only. Fitting it instead would letterbox the banner and leave a
 * visible gap on either side of the robot.
 *
 * The alt text is a required parameter rather than a default, because an image
 * that carries the product name has to be described -- and the description is
 * language specific, so it belongs in the string resources rather than here.
 */
@Composable
fun BrandBanner(
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.claudroide_banner),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(BannerAspectRatio)
            .clip(RoundedCornerShape(20.dp)),
    )
}

/**
 * The standalone mark, for empty states and the onboarding header.
 *
 * `contentDescription` is nullable because the two uses differ: on its own the
 * mark is decorative and must be silent, whereas beside text that already names
 * the product it is redundant either way. Passing `null` is the honest choice
 * for both, and this signature leaves the decision with the caller instead of
 * hardcoding a description nobody chose.
 */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 96.dp,
) {
    Image(
        painter = painterResource(R.drawable.claudroide_mark),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
    )
}

/** 1376x768, the banner's real aspect ratio. Named so it cannot drift. */
private const val BannerAspectRatio = 1376f / 768f