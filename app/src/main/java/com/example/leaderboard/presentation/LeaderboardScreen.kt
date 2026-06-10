package com.example.leaderboard.presentation

import com.example.leaderboard.domain.RankChange
import com.example.leaderboard.domain.RankedPlayer
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.leaderboard.ui.theme.AccentBronze
import com.example.leaderboard.ui.theme.AccentGold
import com.example.leaderboard.ui.theme.AccentGreen
import com.example.leaderboard.ui.theme.AccentRed
import com.example.leaderboard.ui.theme.AccentSilver
import com.example.leaderboard.ui.theme.BgDark
import com.example.leaderboard.ui.theme.FlashColor
import com.example.leaderboard.ui.theme.SurfaceCard
import com.example.leaderboard.ui.theme.SurfaceDark
import com.example.leaderboard.ui.theme.TextPrimary
import com.example.leaderboard.ui.theme.TextSecondary

private val AvatarColors = listOf(
    Color(0xFF6366F1), Color(0xFF0EA5E9), Color(0xFFEC4899),
    Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFF8B5CF6),
    Color(0xFFEF4444), Color(0xFF14B8A6), Color(0xFF3B82F6),
    Color(0xFFA855F7), Color(0xFF22C55E), Color(0xFFE11D48),
    Color(0xFF06B6D4), Color(0xFFF97316), Color(0xFF84CC16),
    Color(0xFF0D9488), Color(0xFF7C3AED), Color(0xFFDB2777),
    Color(0xFF2563EB), Color(0xFFCA8A04)
)


@Composable
fun LeaderboardScreen(
    modifier: Modifier = Modifier,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isCompactScreen = configuration.screenHeightDp < 700 || configuration.screenWidthDp < 360

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(playerCount = leaderboard.size)

            if (leaderboard.isEmpty()) {
                LoadingPlaceholder()
            } else {
                LeaderboardList(
                    players = leaderboard,
                    isCompactScreen = isCompactScreen
                )
            }
        }
    }
}


@Composable
private fun TopBar(playerCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "🏆 Live Leaderboard",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (playerCount > 0) {
                    "Match #4821 · $playerCount players · live"
                } else {
                    "Match #4821 · starting..."
                },
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        // Live pulse indicator
        LiveDot()
    }
}

@Composable
private fun LiveDot() {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            scale.animateTo(1.4f, animationSpec = tween(600))
            scale.animateTo(1f, animationSpec = tween(600))
        }
    }
    Box(
        modifier = Modifier
            .scale(scale.value)
            .size(10.dp)
            .clip(CircleShape)
            .background(AccentGreen)
    )
}


@Composable
private fun LeaderboardList(
    players: List<RankedPlayer>,
    isCompactScreen: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 8.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(if (isCompactScreen) 4.dp else 6.dp)
    ) {
        item(key = "podium") {
            val top3 = players.take(3)
            if (top3.size == 3) {
                PodiumRow(
                    first = top3[0],
                    second = top3[1],
                    third = top3[2],
                    isCompactScreen = isCompactScreen
                )
                Spacer(Modifier.height(if (isCompactScreen) 8.dp else 12.dp))
            }
        }
        itemsIndexed(
            items = players.drop(3),
            key = { _, player -> player.player.id }
        ) { _, rankedPlayer ->
            PlayerRow(
                rankedPlayer = rankedPlayer,
                isCompactScreen = isCompactScreen
            )
        }

    }
}


@Composable
private fun PodiumRow(
    first: RankedPlayer,
    second: RankedPlayer,
    third: RankedPlayer,
    isCompactScreen: Boolean
) {
    val firstHeight = if (isCompactScreen) 56.dp else 80.dp
    val secondHeight = if (isCompactScreen) 44.dp else 60.dp
    val thirdHeight = if (isCompactScreen) 36.dp else 48.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompactScreen) 10.dp else 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            PodiumItem(
                player = second,
                medal = "🥈",
                height = secondHeight,
                isCompactScreen = isCompactScreen
            )
            PodiumItem(
                player = first,
                medal = "👑",
                height = firstHeight,
                isFirst = true,
                isCompactScreen = isCompactScreen
            )
            PodiumItem(
                player = third,
                medal = "🥉",
                height = thirdHeight,
                isCompactScreen = isCompactScreen
            )
        }
    }
}

@Composable
private fun PodiumItem(
    player: RankedPlayer,
    medal: String,
    height: Dp,
    isCompactScreen: Boolean,
    isFirst: Boolean = false
) {
    val animatedScore by animateIntAsState(
        targetValue = player.player.score.toInt(),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "podium_score_${player.player.id}"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(medal, fontSize = if (isFirst) {
            if (isCompactScreen) 20.sp else 24.sp
        } else {
            if (isCompactScreen) 16.sp else 18.sp
        })
        Spacer(Modifier.height(4.dp))
        PlayerAvatar(
            name = player.player.username,
            avatarSeed = player.player.avatarSeed,
            size = when {
                isFirst && isCompactScreen -> 40.dp
                isFirst -> 52.dp
                isCompactScreen -> 36.dp
                else -> 44.dp
            },
            highlighted = player.isRecentlyUpdated
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = player.player.username,
            color = TextPrimary,
            fontSize = if (isCompactScreen) 10.sp else 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "%,d".format(animatedScore),
            color = AccentGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(if (isCompactScreen) 52.dp else 64.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(
                    when (player.rank) {
                        1 -> AccentGold.copy(alpha = 0.2f)
                        2 -> AccentSilver.copy(alpha = 0.15f)
                        else -> AccentBronze.copy(alpha = 0.15f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#${player.rank}",
                color = when (player.rank) {
                    1 -> AccentGold
                    2 -> AccentSilver
                    else -> Color(0xFFD97706)
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
private fun PlayerRow(
    rankedPlayer: RankedPlayer,
    isCompactScreen: Boolean
) {
    val rowBg by animateColorAsState(
        targetValue = if (rankedPlayer.isRecentlyUpdated) FlashColor else Color.Transparent,
        animationSpec = tween(durationMillis = 600),
        label = "row_flash_${rankedPlayer.player.id}"
    )
    val animatedScore by animateIntAsState(
        targetValue = rankedPlayer.player.score.toInt(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "score_${rankedPlayer.player.id}"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(rowBg)
                .padding(
                    horizontal = 12.dp,
                    vertical = if (isCompactScreen) 8.dp else 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "${rankedPlayer.rank}",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(24.dp)
            )
            PlayerAvatar(
                name = rankedPlayer.player.username,
                avatarSeed = rankedPlayer.player.avatarSeed,
                size = if (isCompactScreen) 32.dp else 36.dp,
                highlighted = rankedPlayer.isRecentlyUpdated
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rankedPlayer.player.username,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                RankChangeIndicator(rankedPlayer.rankChange)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "%,d".format(animatedScore),
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (rankedPlayer.isRecentlyUpdated && rankedPlayer.lastScoreDelta > 0) {
                    Text(
                        text = "+${rankedPlayer.lastScoreDelta}",
                        color = AccentGreen,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}


@Composable
private fun RankChangeIndicator(change: RankChange) {
    val (symbol, color) = when (change) {
        RankChange.UP   -> "▲ moved up"   to AccentGreen
        RankChange.DOWN -> "▼ moved down" to AccentRed
        RankChange.SAME -> "● same rank"  to TextSecondary
        RankChange.NONE -> ""             to Color.Transparent
    }
    if (symbol.isNotEmpty()) {
        Text(text = symbol, color = color, fontSize = 11.sp)
    }
}


@Composable
private fun PlayerAvatar(
    name: String,
    avatarSeed: Int,
    size: Dp,
    highlighted: Boolean
) {
    val bgColor = AvatarColors[avatarSeed % AvatarColors.size]
    val borderColor by animateColorAsState(
        targetValue = if (highlighted) AccentGreen else Color.Transparent,
        animationSpec = tween(300),
        label = "avatar_border_$avatarSeed"
    )
    val initials = name.filter { it.isUpperCase() || it.isDigit() }
        .take(2)
        .ifEmpty { name.take(2).uppercase() }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(borderColor)
            .padding(if (highlighted) 2.dp else 0.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = (size.value * 0.33f).sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}


@Composable
private fun LoadingPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Starting match...", color = TextSecondary, fontSize = 14.sp)
    }
}