package ngn.yzg.swc.entity;

public class CrawlerException extends Exception {
	private static final long serialVersionUID = 1L;

	public CrawlerException() {}
	
	public CrawlerException(String info) {
		super(info);
	}
}
