package common;

import java.io.Serializable;

public class Message implements Serializable {

	private static final long serialVersionUID = 781204753323720017L;

	private String id;
	private MsgCommand cmd;
	private String detail;
	private int v;
	private int serialNo;

	public int getSerialNo() {
		return serialNo;
	}

	public void setSerialNo(int seriousNo) {
		this.serialNo = seriousNo;
	}

	public int getV() {
		return v;
	}

	public void setV(int v) {
		this.v = v;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public MsgCommand getCmd() {
		return cmd;
	}

	public void setCmd(MsgCommand cmd) {
		this.cmd = cmd;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	/**
	 * compare two message, if they have same ids and same serial numbers,
	 * return true; else return false;
	 * 
	 * @param msg
	 * @return
	 */
	public boolean msgEquals(Message msg) {
		if (this.getId().equals(msg.getId())
				&& this.getSerialNo() == msg.getSerialNo())
			return true;
		else
			return false;
	}

	public String msgToString() {
		return String.format(" %s,%s,%d ", getId(), getDetail(), getSerialNo());
	}

}