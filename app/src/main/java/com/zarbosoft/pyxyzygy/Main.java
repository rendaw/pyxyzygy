package com.zarbosoft.pyxyzygy;

public class Main {
	/*
	enum Whatever implements ByteBuddyAgent.AttachmentProvider {
		INSTANCE;

		@Override
		public Accessor attempt() {
			return
					uncheck(() -> (ByteBuddyAgent.AttachmentProvider.Accessor) Class
							.forName(
									"net.bytebuddy.agent.ByteBuddyAgent$AttachmentProvider$Accessor$Simple$WithExternalAttachment")
							.getDeclaredConstructor(Class.class, List.class)
							.newInstance(
									ClassLoader.getSystemClassLoader().loadClass(VIRTUAL_MACHINE_TYPE_NAME),
									ImmutableList.of()
							));
		}
	}
	static {
		com.sun.tools.attach.VirtualMachine.class.getSimpleName();
		java.lang.instrument.Instrumentation.class.getSimpleName();
		final String pid;
		{
			String runtimeName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
			int processIdIndex = runtimeName.indexOf('@');
				pid= runtimeName.substring(0, processIdIndex);
		}
		final String agentJarPath;
		{
			InputStream inputStream = Installer.class.getResourceAsStream('/' + Installer.class.getName().replace('.', '/') + ".class");
			if (inputStream == null) {
				throw new IllegalStateException("Cannot locate class file for Byte Buddy installer");
			}
			try {
				File agentJar = File.createTempFile("byteBuddyAgent", ".jar");
				agentJar.deleteOnExit(); // Agent jar is required until VM shutdown due to lazy class loading.
				Manifest manifest = new Manifest();
				manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
				manifest.getMainAttributes().put(new Attributes.Name("Agent-Class"), Installer.class.getName());
				manifest.getMainAttributes().put(new Attributes.Name("Can-Redefine-Classes"), Boolean.TRUE.toString());
				manifest.getMainAttributes().put(new Attributes.Name("Can-Retransform-Classes"), Boolean.TRUE.toString());
				manifest.getMainAttributes().put(new Attributes.Name("Can-Set-Native-Method-Prefix"), Boolean.TRUE.toString());
				JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(agentJar), manifest);
				try {
					jarOutputStream.putNextEntry(new JarEntry(Installer.class.getName().replace('.', '/') + ".class"));
					byte[] buffer = new byte[BUFFER_SIZE];
					int index;
					while ((index = inputStream.read(buffer)) != END_OF_FILE) {
						jarOutputStream.write(buffer, START_INDEX, index);
					}
					jarOutputStream.closeEntry();
				} finally {
					jarOutputStream.close();
				}
				return agentJar;
			} finally {
				inputStream.close();
			}
		}
		VirtualMachine.attach(pid).loadAgent();
		new AgentBuilder.Default()
				.type(ElementMatchers.named("javafx.scene.image.ImageView"))
				.transform((builder, typeDescription, classLoader, module) -> new ByteBuddy()
						.rebase(typeDescription, ClassFileLocator.ForClassLoader.of(classLoader))
						.defineMethod("doCreatePeerPublic", NGImageView.class, Visibility.PUBLIC)
						.intercept(MethodDelegation.toConstructor(NGImageView.class))
						.method(named("doCreatePeer"))
						.intercept(MethodCall.invoke(named("doCreatePeerPublic"))))
				.installOn(uncheck(()->(Instrumentation) ClassLoader.getSystemClassLoader()
				.loadClass(Installer.class.getName())
				.getMethod("getInstrumentation")
				.invoke(null)));
		ByteBuddyAgent.install(Whatever.INSTANCE);

		//
		final DynamicType.Unloaded<ImageView> fixedImageView = new ByteBuddy()
				.rebase(ImageView.class)
				.defineMethod("doCreatePeerPublic", NGImageView.class, Visibility.PUBLIC)
				.intercept(MethodDelegation.toConstructor(NGImageView.class))
				.method(named("doCreatePeer"))
				.intercept(MethodCall.invoke(named("doCreatePeerPublic")))
				.make();
		uncheck(() -> fixedImageView.saveIn(new File("bb2.class")));
		fixedImageView.load(ClassLoader.getSystemClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
	}

	*/
	public static void main(String[] args) {
		Launch.main(args);
	}
}
