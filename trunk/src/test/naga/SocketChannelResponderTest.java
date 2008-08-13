package naga;
/**
 * @author Christoffer Lerno 
 * @version $Revision$ $Date$   $Author$
 */

import junit.framework.TestCase;
import org.easymock.classextension.EasyMock;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

@SuppressWarnings({"StaticMethodReferencedViaSubclass"})
public class SocketChannelResponderTest extends TestCase
{
	SocketChannelResponder m_socketChannelResponder;
	SelectionKey m_key;
	SocketChannel m_channel;

	protected void setUp() throws Exception
	{
		m_channel = EasyMock.createMock(SocketChannel.class);
		m_key = EasyMock.createMock(SelectionKey.class);
	}

	public void testWriteExceedingMax()
	{
		EasyMock.expect(m_channel.isConnected()).andReturn(true).once();
		EasyMock.expect(m_key.interestOps(0)).andReturn(m_key).once();
		replay();
		m_socketChannelResponder = new SocketChannelResponder(m_channel);
		m_socketChannelResponder.setKey(m_key);
		m_socketChannelResponder.setMaxQueueSize(3);
		assertEquals(0, m_socketChannelResponder.getBytesWritten());
		assertEquals(0, m_socketChannelResponder.getWriteQueueSize());
		verify();
		reset();

		EasyMock.expect(m_key.interestOps(SelectionKey.OP_WRITE)).andReturn(m_key).once();
		replay();

		// Add a small packet
		assertEquals(true, m_socketChannelResponder.write("F!".getBytes()));
		verify();

		// This fails because the queue would be too big.
		assertEquals(false, m_socketChannelResponder.write("OO".getBytes()));
	}

	public void testWrite() throws Exception
	{

		// Open a writer.

		PacketWriter writer = EasyMock.createMock(PacketWriter.class);
		EasyMock.expect(m_channel.isConnected()).andReturn(true).once();
		EasyMock.expect(m_key.interestOps(0)).andReturn(m_key).once();
		replay();
		m_socketChannelResponder = new SocketChannelResponder(m_channel);
		m_socketChannelResponder.setKey(m_key);
		m_socketChannelResponder.setPacketWriter(writer);
		assertEquals(0, m_socketChannelResponder.getBytesWritten());
		assertEquals(0, m_socketChannelResponder.getWriteQueueSize());

		verify();
		reset();
		EasyMock.expect(m_key.interestOps(SelectionKey.OP_WRITE)).andReturn(m_key).once();
		replay();

		// Add a packet
		byte[] packet = "FOO!".getBytes();
		m_socketChannelResponder.write(packet);

		assertEquals(0, m_socketChannelResponder.getBytesWritten());
		assertEquals(4, m_socketChannelResponder.getWriteQueueSize());

		verify();
		reset();

		// Write nothing of the packet.
		EasyMock.expect(m_key.interestOps(0)).andReturn(m_key).once();
		EasyMock.expect(m_key.interestOps(SelectionKey.OP_WRITE)).andReturn(m_key).once();
		EasyMock.expect(m_channel.write((ByteBuffer) null)).andReturn(0).once();
		EasyMock.expect(writer.isEmpty()).andReturn(true).once();
		EasyMock.expect(writer.isEmpty()).andReturn(false).times(2);
		EasyMock.expect(writer.getBuffer()).andReturn(null).once();
		writer.setPacket(packet);
		EasyMock.expectLastCall().once();

		replay();
		EasyMock.replay(writer);

		m_socketChannelResponder.notifyCanWrite();

		assertEquals(0, m_socketChannelResponder.getBytesWritten());
		assertEquals(0, m_socketChannelResponder.getWriteQueueSize());

		EasyMock.verify(writer);
		EasyMock.reset(writer);
		verify();
		reset();

		// Write part of the packet.
		EasyMock.expect(m_key.interestOps(0)).andReturn(m_key).once();
		EasyMock.expect(m_key.interestOps(SelectionKey.OP_WRITE)).andReturn(m_key).once();
		EasyMock.expect(m_channel.write((ByteBuffer) null)).andReturn(3).once();
		EasyMock.expect(m_channel.write((ByteBuffer) null)).andReturn(0).once();
		EasyMock.expect(writer.isEmpty()).andReturn(false).times(5);
		EasyMock.expect(writer.getBuffer()).andReturn(null).times(2);
		replay();
		EasyMock.replay(writer);

		m_socketChannelResponder.notifyCanWrite();

		assertEquals(3, m_socketChannelResponder.getBytesWritten());
		assertEquals(0, m_socketChannelResponder.getWriteQueueSize());

		EasyMock.verify(writer);
		EasyMock.reset(writer);
		verify();
		reset();

		// Finish writing the packet.
		EasyMock.expect(m_key.interestOps(0)).andReturn(m_key).once();
		EasyMock.expect(m_channel.write((ByteBuffer) null)).andReturn(1).once();
		EasyMock.expect(writer.isEmpty()).andReturn(false).times(3);
		EasyMock.expect(writer.isEmpty()).andReturn(true).times(3);
		EasyMock.expect(writer.getBuffer()).andReturn(null).times(1);
		replay();
		EasyMock.replay(writer);

		m_socketChannelResponder.notifyCanWrite();
		assertEquals(4, m_socketChannelResponder.getBytesWritten());
		assertEquals(0, m_socketChannelResponder.getWriteQueueSize());

		EasyMock.verify(writer);
		EasyMock.reset(writer);
		verify();
		reset();

		// Test Empty read
		EasyMock.expect(m_key.interestOps(0)).andReturn(m_key).once();
		EasyMock.expect(writer.isEmpty()).andReturn(true).times(2);
		replay();
		EasyMock.replay(writer);

		m_socketChannelResponder.notifyCanWrite();

		EasyMock.verify(writer);
		verify();

	}

	private void reset()
	{
		EasyMock.reset(m_channel);
		EasyMock.reset(m_key);
	}

	private void verify()
	{
		EasyMock.verify(m_channel);
		EasyMock.verify(m_key);
	}

	private void replay()
	{
		EasyMock.replay(m_channel);
		EasyMock.replay(m_key);
	}

	public void testFinishConnectThrowsException() throws IOException
	{
		EasyMock.expect(m_key.interestOps(SelectionKey.OP_CONNECT)).andReturn(m_key).once();
		m_key.cancel();
		EasyMock.expectLastCall().once();
		replay();
		m_socketChannelResponder = new SocketChannelResponder(SocketChannel.open());
		m_socketChannelResponder.setKey(m_key);
		m_socketChannelResponder.notifyCanConnect();
		verify();
		reset();
	}

	public void testCanReadThrowsException() throws IOException
	{
		EasyMock.expect(m_key.interestOps(SelectionKey.OP_CONNECT)).andReturn(m_key).once();
		m_key.cancel();
		EasyMock.expectLastCall().once();
		replay();
		m_socketChannelResponder = new SocketChannelResponder(SocketChannel.open());
		m_socketChannelResponder.setKey(m_key);
		m_socketChannelResponder.notifyCanRead();
		verify();
		assertEquals(false, m_socketChannelResponder.isOpen());
	}

	public void testCanWriteThrowsException() throws IOException
	{
		EasyMock.expect(m_key.interestOps(SelectionKey.OP_CONNECT)).andReturn(m_key).times(2);
		EasyMock.expect(m_key.interestOps(SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE)).andReturn(m_key).once();
		m_key.cancel();
		EasyMock.expectLastCall().once();
		replay();
		m_socketChannelResponder = new SocketChannelResponder(SocketChannel.open());
		m_socketChannelResponder.setKey(m_key);
		m_socketChannelResponder.write(new byte[] { 0 });
		m_socketChannelResponder.notifyCanWrite();
		verify();
		assertEquals(false, m_socketChannelResponder.isOpen());
	}

	public void testSetKey() throws Exception
	{
		m_key.cancel();
		EasyMock.expectLastCall().once();
		replay();
		m_socketChannelResponder = new SocketChannelResponder(null);
		m_socketChannelResponder.close();
		m_socketChannelResponder.setKey(m_key);
		verify();
		assertEquals(false, m_socketChannelResponder.isOpen());
	}
}