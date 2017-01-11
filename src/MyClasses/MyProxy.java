package MyClasses;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

public class MyProxy extends Authenticator
{
	private String proxyUser;
	private String proxyPass;

	public MyProxy(String user, String password)
	{
		this.proxyUser = user;
		this.proxyPass = password;
	}

	@Override
	protected PasswordAuthentication getPasswordAuthentication()
	{
		return new PasswordAuthentication(this.proxyUser, this.proxyPass.toCharArray());
	}
}
