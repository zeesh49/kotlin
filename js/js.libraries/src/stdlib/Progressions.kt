package jet

import js.noImpl

library("NumberProgression")
public trait Progression<N: Any>: Iterable<N> {
    native
    public val start: N
    native
    public val end: N
    native
    public val increment: Number
}

library("NumberProgression")
public class IntProgression(
    native
    public override val start: Int,
    native
    public override val end: Int,
    native
    public override val increment: Int
): Progression<Int> {

    native
    override fun iterator(): IntIterator = js.noImpl
}

library("NumberProgression")
public class LongProgression(
    native
    public override val start: Long,
    native
    public override val end: Long,
    native
    public override val increment: Long
): Progression<Long> {

    native
    override fun iterator(): LongIterator = js.noImpl
}

library("NumberProgression")
public class ByteProgression(
    native
    public override val start: Byte,
    native
    public override val end: Byte,
    native
    public override val increment: Int
): Progression<Byte> {

    native
    override fun iterator(): ByteIterator = js.noImpl
}

library("NumberProgression")
public class ShortProgression(
    native
    public override val start: Short,
    native
    public override val end: Short,
    native
    public override val increment: Int
): Progression<Short> {

    native
    override fun iterator(): ShortIterator = js.noImpl
}

library("NumberProgression")
public class CharProgression(
    native
    public override val start: Char,
    native
    public override val end: Char,
    native
    public override val increment: Int
): Progression<Char> {

    native
    override fun iterator(): CharIterator = js.noImpl
}

library("NumberProgression")
public class FloatProgression(
    native
    public override val start: Float,
    native
    public override val end: Float,
    native
    public override val increment: Float
): Progression<Float> {

    native
    override fun iterator(): FloatIterator = js.noImpl
}

library("NumberProgression")
public class DoubleProgression(
    native
    public override val start: Double,
    native
    public override val end: Double,
    native
    public override val increment: Double
): Progression<Double> {

    native
    override fun iterator(): DoubleIterator = js.noImpl
}
