package com.eight87.shutterboy.ui.collections

/**
 * Phase E.4 — fold a user-pinned ordering on top of the natural ordering
 * coming out of the data layer. Pure helper so the chip row + folder
 * grid both apply the same shape:
 *
 *   - Items whose key is present in [persistedKeys] are emitted first,
 *     in `persistedKeys` order.
 *   - Items in [natural] whose key is NOT present in `persistedKeys`
 *     (e.g. a folder that appeared in a fresh scan after the user
 *     pinned a custom order) are appended at the end, preserving
 *     `natural` ordering among themselves — so a *new* folder lands
 *     after the custom-pinned ones, not interleaved.
 *   - Keys in [persistedKeys] but not present in [natural] (e.g. a
 *     folder that was deleted) are dropped silently — no dangling refs.
 *
 * Empty `persistedKeys` collapses to `natural` unchanged. Both inputs
 * preserve caller-side semantics; this helper does no additional sorting.
 *
 * For the smart-album case where `T == K`, pass `keyOf = { it }`.
 */
internal fun <T, K> applyCustomOrder(
    natural: List<T>,
    persistedKeys: List<K>,
    keyOf: (T) -> K,
): List<T> {
    if (persistedKeys.isEmpty()) return natural
    val byKey: Map<K, T> = natural.associateBy(keyOf)
    val pinned = persistedKeys.mapNotNull { byKey[it] }
    val seenKeys = pinned.map(keyOf).toSet()
    val appended = natural.filter { keyOf(it) !in seenKeys }
    return pinned + appended
}
