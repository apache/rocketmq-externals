package org.apache.rocketmq.amqp.framing;

import org.apache.mina.common.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.lang.ref.WeakReference;

/**
 * A short string is a representation of an AMQ Short String
 * Short strings differ from the Java String class by being limited to on ASCII characters (0-127)
 * and thus can be held more effectively in a byte buffer.
 *
 */
public final class AMQShortString implements CharSequence, Comparable<AMQShortString>
{
    /**
     * The maximum number of octets in AMQ short string as defined in AMQP specification
     */
    public static final int MAX_LENGTH = 255;
    private static final byte MINUS = (byte)'-';
    private static final byte ZERO = (byte) '0';

    private final class TokenizerImpl implements AMQShortStringTokenizer
    {
        private final byte _delim;
        private int _count = -1;
        private int _pos = 0;

        public TokenizerImpl(final byte delim)
        {
            _delim = delim;
        }

        public int countTokens()
        {
            if(_count == -1)
            {
                _count = 1 + AMQShortString.this.occurences(_delim);
            }
            return _count;
        }

        public AMQShortString nextToken()
        {
            if(_pos <= AMQShortString.this.length())
            {
                int nextDelim = AMQShortString.this.indexOf(_delim, _pos);
                if(nextDelim == -1)
                {
                    nextDelim = AMQShortString.this.length();
                }

                AMQShortString nextToken = AMQShortString.this.substring(_pos, nextDelim++);
                _pos = nextDelim;
                return nextToken;
            }
            else
            {
                return null;
            }
        }

        public boolean hasMoreTokens()
        {
            return _pos <= AMQShortString.this.length();
        }
    }

    private AMQShortString substring(final int from, final int to)
    {
        return new AMQShortString(_data, from+_offset, to+_offset);
    }


    private static final ThreadLocal<Map<AMQShortString, WeakReference<AMQShortString>>> _localInternMap =
        new ThreadLocal<Map<AMQShortString, WeakReference<AMQShortString>>>()
        {
            protected Map<AMQShortString, WeakReference<AMQShortString>> initialValue()
            {
                return new WeakHashMap<AMQShortString, WeakReference<AMQShortString>>();
            };
        };

    private static final Map<AMQShortString, WeakReference<AMQShortString>> _globalInternMap =
        new WeakHashMap<AMQShortString, WeakReference<AMQShortString>>();

    private static final Logger _logger = LoggerFactory.getLogger(AMQShortString.class);

    private final byte[] _data;
    private final int _offset;
    private int _hashCode;
    private String _asString = null;

    private final int _length;
    private static final char[] EMPTY_CHAR_ARRAY = new char[0];

    public static final AMQShortString EMPTY_STRING = new AMQShortString((String)null);

    public AMQShortString(byte[] data)
    {
        if (data == null)
        {
            throw new NullPointerException("Cannot create AMQShortString with null data[]");
        }
        if (data.length > MAX_LENGTH)
        {
            throw new IllegalArgumentException("Cannot create AMQShortString with number of octets over 255!");
        }
        _data = data.clone();
        _length = data.length;
        _offset = 0;
    }

    public AMQShortString(String data)
    {
        this((data == null) ? EMPTY_CHAR_ARRAY : data.toCharArray());
        _asString = data;
    }

    public AMQShortString(char[] data)
    {
        if (data == null)
        {
            throw new NullPointerException("Cannot create AMQShortString with null char[]");
        }
        // the current implementation of 0.8/0.9.x short string encoding
        // supports only ASCII characters
        if (data.length> MAX_LENGTH)
        {
            throw new IllegalArgumentException("Cannot create AMQShortString with number of octets over 255!");
        }
        final int length = data.length;
        final byte[] stringBytes = new byte[length];
        int hash = 0;
        for (int i = 0; i < length; i++)
        {
            stringBytes[i] = (byte) (0xFF & data[i]);
            hash = (31 * hash) + stringBytes[i];
        }
        _hashCode = hash;
        _data = stringBytes;

        _length = length;
        _offset = 0;

    }

    public AMQShortString(CharSequence charSequence)
    {
        if (charSequence == null)
        {
            // it should be possible to create short string for null data
            charSequence = "";
        }
        // the current implementation of 0.8/0.9.x short string encoding
        // supports only ASCII characters
        if (charSequence.length() > MAX_LENGTH)
        {
            throw new IllegalArgumentException("Cannot create AMQShortString with number of octets over 255!");
        }
        final int length = charSequence.length();
        final byte[] stringBytes = new byte[length];
        int hash = 0;
        for (int i = 0; i < length; i++)
        {
            stringBytes[i] = ((byte) (0xFF & charSequence.charAt(i)));
            hash = (31 * hash) + stringBytes[i];

        }

        _data = stringBytes;
        _hashCode = hash;
        _length = length;
        _offset = 0;

    }

    private AMQShortString(ByteBuffer data, final int length)
    {
        if (length > MAX_LENGTH)
        {
            throw new IllegalArgumentException("Cannot create AMQShortString with number of octets over 255!");
        }
        if(data.isDirect() || data.isReadOnly())
        {
            byte[] dataBytes = new byte[length];
            data.get(dataBytes);
            _data = dataBytes;
            _offset = 0;
        }
        else
        {

            _data = data.array();
            _offset = data.arrayOffset() + data.position();
            data.skip(length);

        }
        _length = length;

    }

    private AMQShortString(final byte[] data, final int from, final int to)
    {
        if (data == null)
        {
            throw new NullPointerException("Cannot create AMQShortString with null data[]");
        }
        int length = to - from;
        if (length > MAX_LENGTH)
        {
            throw new IllegalArgumentException("Cannot create AMQShortString with number of octets over 255!");
        }
        _offset = from;
        _length = length;
        _data = data;
    }

    public AMQShortString shrink()
    {
        if(_data.length != _length)
        {
            byte[] dataBytes = new byte[_length];
            System.arraycopy(_data,_offset,dataBytes,0,_length);
            return new AMQShortString(dataBytes,0,_length);
        }
        else
        {
            return this;
        }
    }

    /**
     * Get the length of the short string
     * @return length of the underlying byte array
     */
    public int length()
    {
        return _length;
    }

    public char charAt(int index)
    {

        return (char) _data[_offset + index];

    }

    public CharSequence subSequence(int start, int end)
    {
        return new CharSubSequence(start, end);
    }

    public static AMQShortString readFromBuffer(ByteBuffer buffer)
    {
        final short length = buffer.getUnsigned();
        if (length == 0)
        {
            return null;
        }
        else
        {

            return new AMQShortString(buffer, length);
        }
    }

    public byte[] getBytes()
    {
        if(_offset == 0 && _length == _data.length)
        {
            return _data.clone();
        }
        else
        {
            byte[] data = new byte[_length];
            System.arraycopy(_data,_offset,data,0,_length);
            return data;
        }
    }

    public void writeToBuffer(ByteBuffer buffer)
    {

        final int size = length();
        //buffer.setAutoExpand(true);
        buffer.put((byte) size);
        buffer.put(_data, _offset, size);

    }

    public boolean endsWith(String s)
    {
        return endsWith(new AMQShortString(s));
    }


    public boolean endsWith(AMQShortString otherString)
    {

        if (otherString.length() > length())
        {
            return false;
        }


        int thisLength = length();
        int otherLength = otherString.length();

        for (int i = 1; i <= otherLength; i++)
        {
            if (charAt(thisLength - i) != otherString.charAt(otherLength - i))
            {
                return false;
            }
        }
        return true;
    }

    public boolean startsWith(String s)
    {
        return startsWith(new AMQShortString(s));
    }

    public boolean startsWith(AMQShortString otherString)
    {

        if (otherString.length() > length())
        {
            return false;
        }

        for (int i = 0; i < otherString.length(); i++)
        {
            if (charAt(i) != otherString.charAt(i))
            {
                return false;
            }
        }

        return true;

    }

    public boolean startsWith(CharSequence otherString)
    {
        if (otherString.length() > length())
        {
            return false;
        }

        for (int i = 0; i < otherString.length(); i++)
        {
            if (charAt(i) != otherString.charAt(i))
            {
                return false;
            }
        }

        return true;
    }


    private final class CharSubSequence implements CharSequence
    {
        private final int _sequenceOffset;
        private final int _end;

        public CharSubSequence(final int offset, final int end)
        {
            _sequenceOffset = offset;
            _end = end;
        }

        public int length()
        {
            return _end - _sequenceOffset;
        }

        public char charAt(int index)
        {
            return AMQShortString.this.charAt(index + _sequenceOffset);
        }

        public CharSequence subSequence(int start, int end)
        {
            return new CharSubSequence(start + _sequenceOffset, end + _sequenceOffset);
        }
    }

    public char[] asChars()
    {
        final int size = length();
        final char[] chars = new char[size];

        for (int i = 0; i < size; i++)
        {
            chars[i] = (char) _data[i + _offset];
        }

        return chars;
    }


    public String asString()
    {
        if (_asString == null)
        {
            _asString = new String(asChars());
        }
        return _asString;
    }

    public boolean equals(Object o)
    {


        if(o instanceof AMQShortString)
        {
            return equals((AMQShortString)o);
        }
        if(o instanceof CharSequence)
        {
            return equals((CharSequence)o);
        }

        if (o == null)
        {
            return false;
        }

        if (o == this)
        {
            return true;
        }


        return false;

    }

    public boolean equals(final AMQShortString otherString)
    {
        if (otherString == this)
        {
            return true;
        }

        if (otherString == null)
        {
            return false;
        }

        final int hashCode = _hashCode;

        final int otherHashCode = otherString._hashCode;

        if ((hashCode != 0) && (otherHashCode != 0) && (hashCode != otherHashCode))
        {
            return false;
        }

        final int length = _length;

        if(length != otherString._length)
        {
            return false;
        }


        final byte[] data = _data;

        final byte[] otherData = otherString._data;

        final int offset = _offset;

        final int otherOffset = otherString._offset;

        if(offset == 0 && otherOffset == 0 && length == data.length && length == otherData.length)
        {
            return Arrays.equals(data, otherData);
        }
        else
        {
            int thisIdx = offset;
            int otherIdx = otherOffset;
            for(int i = length;  i-- != 0; )
            {
                if(!(data[thisIdx++] == otherData[otherIdx++]))
                {
                    return false;
                }
            }
        }

        return true;

    }

    public boolean equals(CharSequence s)
    {
        if(s instanceof AMQShortString)
        {
            return equals((AMQShortString)s);
        }

        if (s == null)
        {
            return false;
        }

        if (s.length() != length())
        {
            return false;
        }

        for (int i = 0; i < length(); i++)
        {
            if (charAt(i) != s.charAt(i))
            {
                return false;
            }
        }

        return true;
    }

    public int hashCode()
    {
        int hash = _hashCode;
        if (hash == 0)
        {
            final int size = length();

            for (int i = 0; i < size; i++)
            {
                hash = (31 * hash) + _data[i+_offset];
            }

            _hashCode = hash;
        }

        return hash;
    }

    public void setDirty()
    {
        _hashCode = 0;
    }

    public String toString()
    {
        return asString();
    }

    public int compareTo(AMQShortString name)
    {
        if (name == null)
        {
            return 1;
        }
        else
        {

            if (name.length() < length())
            {
                return -name.compareTo(this);
            }

            for (int i = 0; i < length(); i++)
            {
                final byte d = _data[i+_offset];
                final byte n = name._data[i+name._offset];
                if (d < n)
                {
                    return -1;
                }

                if (d > n)
                {
                    return 1;
                }
            }

            return (length() == name.length()) ? 0 : -1;
        }
    }


    public AMQShortStringTokenizer tokenize(byte delim)
    {
        return new TokenizerImpl(delim);
    }


    public AMQShortString intern()
    {

        hashCode();

        Map<AMQShortString, WeakReference<AMQShortString>> localMap =
            _localInternMap.get();

        WeakReference<AMQShortString> ref = localMap.get(this);
        AMQShortString internString;

        if(ref != null)
        {
            internString = ref.get();
            if(internString != null)
            {
                return internString;
            }
        }


        synchronized(_globalInternMap)
        {

            ref = _globalInternMap.get(this);
            if((ref == null) || ((internString = ref.get()) == null))
            {
                internString = shrink();
                ref = new WeakReference(internString);
                _globalInternMap.put(internString, ref);
            }

        }
        localMap.put(internString, ref);
        return internString;

    }

    private int occurences(final byte delim)
    {
        int count = 0;
        final int end = _offset + _length;
        for(int i = _offset ; i < end ; i++ )
        {
            if(_data[i] == delim)
            {
                count++;
            }
        }
        return count;
    }

    private int indexOf(final byte val, final int pos)
    {

        for(int i = pos; i < length(); i++)
        {
            if(_data[_offset+i] == val)
            {
                return i;
            }
        }
        return -1;
    }


    public static AMQShortString join(final Collection<AMQShortString> terms,
        final AMQShortString delim)
    {
        if(terms.size() == 0)
        {
            return EMPTY_STRING;
        }

        int size = delim.length() * (terms.size() - 1);
        for(AMQShortString term : terms)
        {
            size += term.length();
        }

        if (size > MAX_LENGTH)
        {
            throw new IllegalArgumentException("Cannot create AMQShortString with number of octets over 255!");
        }
        byte[] data = new byte[size];
        int pos = 0;
        final byte[] delimData = delim._data;
        final int delimOffset = delim._offset;
        final int delimLength = delim._length;


        for(AMQShortString term : terms)
        {

            if(pos!=0)
            {
                System.arraycopy(delimData, delimOffset,data,pos, delimLength);
                pos+=delimLength;
            }
            System.arraycopy(term._data,term._offset,data,pos,term._length);
            pos+=term._length;
        }



        return new AMQShortString(data,0,size);
    }

    public int toIntValue()
    {
        int pos = _offset;
        int val = 0;


        boolean isNegative = (_data[pos] == MINUS);
        if(isNegative)
        {
            pos++;
        }

        final int end = _length + _offset;

        while(pos < end)
        {
            int digit = (int) (_data[pos++] - ZERO);
            if((digit < 0) || (digit > 9))
            {
                throw new NumberFormatException("\""+toString()+"\" is not a valid number");
            }
            val = val * 10;
            val += digit;
        }
        if(isNegative)
        {
            val = val * -1;
        }
        return val;
    }

    public boolean contains(final byte b)
    {
        final int end = _length + _offset;
        for(int i = _offset; i < end; i++)
        {
            if(_data[i] == b)
            {
                return true;
            }
        }
        return false;  //To change body of created methods use File | Settings | File Templates.
    }

    public static AMQShortString valueOf(Object obj)
    {
        return obj == null ? null : new AMQShortString(String.valueOf(obj));
    }


    public static void main(String args[])
    {
        AMQShortString s = new AMQShortString("a.b.c.d.e.f.g.h.i.j.k");
        AMQShortString s2 = s.substring(2, 7);

        AMQShortStringTokenizer t = s2.tokenize((byte) '.');
        while(t.hasMoreTokens())
        {
            System.err.println(t.nextToken());
        }
    }

    /**
     * Given any string, this method returns the lower case representation of that.
     *
     * @param stringToConvert the original string
     * @return the lower case representation of the the given string
     */
    public static AMQShortString toLowerCase(AMQShortString stringToConvert) {
        if (null != stringToConvert) {
            return new AMQShortString(stringToConvert.asString().toLowerCase());
        } else {
            return null;
        }
    }

}
