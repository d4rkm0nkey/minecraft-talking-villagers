package me.teajay.talkingvillagers.core.voice;

/**
 * How often an event may be voiced.
 *
 * @param cooldownTicks minimum time between two lines for this event
 * @param chance        probability that the event is voiced at all
 * @param interrupts    the line may cut off a line the entity is still saying
 */
public record EventPolicy(int cooldownTicks, double chance, boolean interrupts) {
    public static final EventPolicy ALWAYS = new EventPolicy(0, 1.0, false);
}
