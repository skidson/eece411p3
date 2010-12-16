package ca.ubc.ece.nio.crawler;

public class SteveDriver {

	public static void main(String[] args) {
		IPCache cache = new IPCache();
		cache.cache("nis-planet1.doshisha.ac.jp");
		cache.cache("planck227ple.test.ibbt.be");
		cache.cache("aladdin.planetlab.extranet.uni-passau.de");
		System.out.println(cache.toString());
	}

}
