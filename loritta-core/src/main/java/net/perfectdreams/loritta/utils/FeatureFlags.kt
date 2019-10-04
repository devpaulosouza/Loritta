package net.perfectdreams.loritta.utils

import com.mrpowergamerbr.loritta.utils.loritta

object FeatureFlags {
	val NEW_WEBSITE_PORT = isEnabled(Names.NEW_WEBSITE_PORT)
	val MEMBER_COUNTER_UPDATE = isEnabled(Names.MEMBER_COUNTER_UPDATE)
	val ALLOW_MORE_THAN_ONE_COUNTER_FOR_PREMIUM_USERS = isEnabled(Names.ALLOW_MORE_THAN_ONE_COUNTER_FOR_PREMIUM_USERS)
	val BOTS_CAN_HAVE_FUN_IN_THE_RAFFLE_TOO = isEnabled(Names.BOTS_CAN_HAVE_FUN_IN_THE_RAFFLE_TOO)
	val WRECK_THE_RAFFLE_STOP_THE_WHALES = isEnabled(Names.WRECK_THE_RAFFLE_STOP_THE_WHALES)
	val SELECT_LOW_BETTING_USERS = isEnabled(Names.SELECT_LOW_BETTING_USERS)
	val SELECT_USERS_WITH_LESS_MONEY = isEnabled(Names.SELECT_USERS_WITH_LESS_MONEY)
	val ADVERTISE_SPARKLYPOWER = isEnabled(Names.ADVERTISE_SPARKLYPOWER)
	val ADVERTISE_SPONSORS = isEnabled(Names.ADVERTISE_SPONSORS)

	fun isEnabled(name: String): Boolean {
		return loritta.config.loritta.featureFlags.contains(name)
	}

	object Names {
		const val NEW_WEBSITE_PORT = "new-website-port"
		const val MEMBER_COUNTER_UPDATE = "member-counter-update"
		const val ALLOW_MORE_THAN_ONE_COUNTER_FOR_PREMIUM_USERS = "allow-more-than-one-counter-for-premium-users"
		const val BOTS_CAN_HAVE_FUN_IN_THE_RAFFLE_TOO = "bots-can-have-fun-in-the-raffle-too"
		const val WRECK_THE_RAFFLE_STOP_THE_WHALES = "wreck-the-raffle"
		const val SELECT_LOW_BETTING_USERS = "$WRECK_THE_RAFFLE_STOP_THE_WHALES-select-low-betting-users"
		const val SELECT_USERS_WITH_LESS_MONEY = "$WRECK_THE_RAFFLE_STOP_THE_WHALES-select-users-with-less-money"
		const val ADVERTISE_SPARKLYPOWER = "advertise-sparklypower"
		const val ADVERTISE_SPONSORS = "advertise-sponsors"
	}
}