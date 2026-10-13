import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.FailableFunction;
import org.apache.commons.lang3.function.FailableSupplier;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.j256.simplemagic.ContentType;

import io.github.toolfactory.narcissus.Narcissus;

public class JarUsedClassReport {

	public static void main(final String[] args) throws IOException {
		//
		if (args == null || args.length < 1) {
			//
			return;
			//
		} // if
			//
		final File file = testAndApply(Objects::nonNull, ArrayUtils.get(args, 0), File::new, null);
		//
		if (file == null || !file.exists() || !file.isFile() || !file.canRead()) {
			//
			return;
			//
		} // if
			//
		final ContentInfo ci = new ContentInfoUtil().findMatch(file);
		//
		if (!Objects.equals(getContentType(ci), ContentType.ZIP)) {
			//
			return;
			//
		} // if
			//
		try (final ZipFile zipFile = new ZipFile(file)) {
			//
			System.out.println(perform(0,
					() -> testAndApply(Objects::nonNull,
							zipFile.getEntry(
									StringUtils.joinWith(".", replace(getMainClass(zipFile), '.', '/'), "class")),
							x -> zipFile.getInputStream(x), null)));
			//
		} // try
			//
	}

	private static Multimap<Integer, String> perform(final int index,
			final FailableSupplier<InputStream, IOException> failableSupplier) throws IOException {
		//
		Multimap<Integer, String> multimap = null;
		//
		try (final InputStream is = failableSupplier != null ? failableSupplier.get() : null) {
			//
			final ClassReader cr = testAndApply(Objects::nonNull, is, ClassReader::new, null);
			//
			if (cr != null) {
				//
				final ClassVisitorImpl cvi = new ClassVisitorImpl();
				//
				final FieldVisitorImpl fvi = new FieldVisitorImpl();
				//
				cvi.fvi = fvi;
				//
				final MethodVisitorImpl mvi = new MethodVisitorImpl();
				//
				cvi.mvi = mvi;
				//
				cr.accept(cvi, 0);
				//
				setNullIfClassInMoudleNames(cvi, "java.desktop", "java.base");
				//
				testAndAccept((a, b) -> a != null && !containsValue(b, a), cvi.name,
						multimap = ObjectUtils.getIfNull(multimap, LinkedHashMultimap::create),
						(a, b) -> put(b, Integer.valueOf(index), a));
				//
				testAndAccept((a, b) -> a != null && !containsValue(b, a), cvi.superName,
						multimap = ObjectUtils.getIfNull(multimap, LinkedHashMultimap::create),
						(a, b) -> put(b, Integer.valueOf(index + 1), a));
				//
				for (int i = 0; cvi.interfaces != null && i < cvi.interfaces.length; i++) {
					//
					testAndAccept((a, b) -> a != null && !containsValue(b, a), ArrayUtils.get(cvi.interfaces, i),
							multimap = ObjectUtils.getIfNull(multimap, LinkedHashMultimap::create),
							(a, b) -> put(b, Integer.valueOf(index + 1), a));
					//
				} // for
					//
				for (int i = 0; cvi.fieldTypes != null && i < cvi.fieldTypes.length; i++) {
					//
					testAndAccept((a, b) -> a != null && !containsValue(b, a), ArrayUtils.get(cvi.fieldTypes, i),
							multimap = ObjectUtils.getIfNull(multimap, LinkedHashMultimap::create),
							(a, b) -> put(b, Integer.valueOf(index + 1), a));
					//
				} // for
					//
				InnerClass ic = null;
				//
				for (int i = 0; cvi.innerClasses != null && i < cvi.innerClasses.length; i++) {
					//
					if ((ic = ArrayUtils.get(cvi.innerClasses, i)) == null) {
						//
						continue;
						//
					} // if
						//
					testAndAccept((a, b) -> a != null && !containsValue(b, a), ic.name,
							multimap = ObjectUtils.getIfNull(multimap, LinkedHashMultimap::create),
							(a, b) -> put(b, Integer.valueOf(index + 1), a));
					//
				} // for
					//
				Collection<String> collection = getRequiredClassNames(cvi.methods, "java.desktop", "java.base",
						"java.management");
				//
				for (int i = 0; i < IterableUtils.size(collection); i++) {
					//
					testAndAccept((a, b) -> a != null && !containsValue(b, a), IterableUtils.get(collection, i),
							multimap = ObjectUtils.getIfNull(multimap, LinkedHashMultimap::create),
							(a, b) -> put(b, Integer.valueOf(index + 1), a));
					//
				} // for
					//
				collection = removeIfClassInMoudleNames(toArray(mvi.classNames, String[]::new), "java.desktop",
						"java.base", "java.management");
				//
				for (int i = 0; i < IterableUtils.size(collection); i++) {
					//
					testAndAccept((a, b) -> a != null && !containsValue(b, a), IterableUtils.get(collection, i),
							multimap = ObjectUtils.getIfNull(multimap, LinkedHashMultimap::create),
							(a, b) -> put(b, Integer.valueOf(index + 1), a));
					//
				} // for
					//
				collection = removeIfClassInMoudleNames(toArray(fvi.classNames, String[]::new));
				//
				for (int i = 0; i < IterableUtils.size(collection); i++) {
					//
					testAndAccept((a, b) -> a != null && !containsValue(b, a), IterableUtils.get(collection, i),
							multimap = ObjectUtils.getIfNull(multimap, LinkedHashMultimap::create),
							(a, b) -> put(b, Integer.valueOf(index + 1), a));
					//
				} // for
					//
			} // if
				//
		} // try
			//
		return multimap;
		//
	}

	private static <T, U> void testAndAccept(final BiPredicate<T, U> predicate, final T t, final U u,
			final BiConsumer<T, U> consumer) {
		if (predicate != null && predicate.test(t, u) && consumer != null) {
			consumer.accept(t, u);
		}
	}

	private static boolean containsValue(final Multimap<?, ?> instance, final Object value) {
		return instance != null && instance.containsValue(value);
	}

	private static <K, V> void put(final Multimap<K, V> instance, final K key, final V value) {
		if (instance != null) {
			instance.put(key, value);
		}
	}

	private static Collection<String> getRequiredClassNames(final Method[] ms, final String... javaModuleNames) {
		//
		Collection<String> names = null;
		//
		Method m = null;
		//
		String descriptor = null;
		//
		Collection<Type> declaredTypes = null;
		//
		Type returnType, argumentType = null;
		//
		String toString, substringBetween, argumentTypeToString, exception = null;
		//
		Type[] argumentTypes = null;
		//
		for (int i = 0; ms != null && i < ms.length; i++) {
			//
			if ((m = ArrayUtils.get(ms, i)) == null || (descriptor = m.descriptor) == null) {
				//
				continue;
				//
			} // if
				//
			if (declaredTypes == null) {
				//
				declaredTypes = toList(map(
						filter(stream(testAndApply(Objects::nonNull, Type.class, FieldUtils::getAllFieldsList, null)),
								f -> f != null && Objects.equals(f.getType(), Type.class)
										&& Modifier.isStatic(f.getModifiers())),
						f -> cast(Type.class, Narcissus.getStaticField(f))));
				//
			} // if
				//
			if (!contains(declaredTypes, returnType = Type.getReturnType(descriptor))) {
				//
				if ((toString = toString(returnType)) != null && toString.startsWith("L") && toString.endsWith(";")) {
					//
					if (!ArrayUtils.contains(javaModuleNames,
							getName(getModule(forName(replace(
									substringBetween = StringUtils.substringBetween(toString, "L", ";"), '/', '.')))))
							&& !contains(names = ObjectUtils.getIfNull(names, ArrayList::new), substringBetween)) {
						//
						add(names, substringBetween);
						//
					} // if
						//
				} else {
					//
					throw new RuntimeException(toString);
					//
				} // if
					//
			} // if
				//
			argumentTypes = Type.getArgumentTypes(descriptor);
			//
			for (int j = 0; argumentTypes != null && j < argumentTypes.length; j++) {
				//
				if (contains(declaredTypes, argumentType = ArrayUtils.get(argumentTypes, j))
						|| (argumentTypeToString = toString(argumentType)) == null) {
					//
					continue;
					//
				} // if
					//
				if (argumentTypeToString.startsWith("[L") && argumentTypeToString.endsWith(";")) {
					//
					if (!ArrayUtils.contains(javaModuleNames,
							getName(getModule(forName(replace(
									substringBetween = StringUtils.substringBetween(argumentTypeToString, "[L", ";"),
									'/', '.')))))
							&& !contains(names = ObjectUtils.getIfNull(names, ArrayList::new), substringBetween)) {
						//
						add(names, substringBetween);
						//
					} // if
						//
				} else if (argumentTypeToString.startsWith("L") && argumentTypeToString.endsWith(";")) {
					//
					if (!ArrayUtils.contains(javaModuleNames,
							getName(getModule(forName(replace(
									substringBetween = StringUtils.substringBetween(argumentTypeToString, "L", ";"),
									'/', '.')))))
							&& !contains(names = ObjectUtils.getIfNull(names, ArrayList::new), substringBetween)) {
						//
						add(names, substringBetween);
						//
					} // if
						//
				} else {
					//
					throw new RuntimeException(argumentTypeToString);
					//
				} // if
					//
			} // for
				//
			for (int j = 0; m.exceptions != null && j < m.exceptions.length; j++) {
				//
				if (ArrayUtils.contains(javaModuleNames,
						getName(getModule(forName(replace(exception = ArrayUtils.get(m.exceptions, j), '/', '.')))))
						|| contains(names = ObjectUtils.getIfNull(names, ArrayList::new), exception)) {
					//
					continue;
					//
				} // if
					//
				throw new RuntimeException(exception);
				//
			} // for
				//
		} // for
			//
		return names;
		//
	}

	private static <E> void add(final Collection<E> instance, final E item) {
		if (instance != null) {
			instance.add(item);
		}
	}

	private static boolean contains(final Collection<?> instance, final Object item) {
		return instance != null && instance.contains(item);
	}

	private static <T> T cast(final Class<T> clz, final Object instance) {
		return clz != null && clz.isInstance(instance) ? clz.cast(instance) : null;
	}

	private static void setNullIfClassInMoudleNames(final Object instance, final String... moduleNames) {
		//
		forEach(stream(testAndApply(Objects::nonNull, getClass(instance), FieldUtils::getAllFieldsList, null)), f -> {
			//
			if (f == null) {
				//
				return;
				//
			} // if
				//
			if (moduleNames != null) {
				//
				final Object value = f != null ? (Modifier.isStatic(f.getModifiers()) ? Narcissus.getStaticField(f)
						: Narcissus.getField(instance, f)) : null;
				//
				if (ArrayUtils.contains(moduleNames, getName(getModule(forName(replace(toString(value), '/', '.')))))) {
					//
					Narcissus.setField(instance, f, null);
					//
				} else if (value instanceof String[] ss) {
					//
					Narcissus.setField(instance, f,
							toArray(removeIfClassInMoudleNames(ss, moduleNames), String[]::new));
					//
				} else if (value instanceof InnerClass[] ics) {
					//
					final List<InnerClass> list = testAndApply(Objects::nonNull,
							testAndApply(Objects::nonNull, ics, Arrays::asList, null), ArrayList::new, null);
					//
					InnerClass ic = null;
					//
					for (int i = IterableUtils.size(list) - 1; i >= 0; i--) {
						//
						if ((ic = ArrayUtils.get(ics, i)) == null) {
							//
							continue;
							//
						} // if
							//
						if (ArrayUtils.contains(moduleNames,
								getName(getModule(forName(replace(ic.outerName, '/', '.')))))) {
							//
							list.remove(i);
							//
						} // if
							//
					} // for
						//
					Narcissus.setField(instance, f, list != null ? list.toArray(InnerClass[]::new) : null);
					//
				} else if (value instanceof Method[] ms) {
					//
					Method m = null;
					//
					for (int i = (ms != null ? ms.length : 0) - 1; i >= 0; i--) {
						//
						if ((m = ArrayUtils.get(ms, i)) == null) {
							//
							continue;
							//
						} // if
							//
						Narcissus.setField(m, FieldUtils.getDeclaredField(getClass(m), "exceptions", true),
								toArray(removeIfClassInMoudleNames(m.exceptions, moduleNames), String[]::new));
						//
					} // for
						//
				} // if
					//
			} // if
				//
		});
		//
	}

	private static <T> T[] toArray(final Collection<T> instance, final IntFunction<T[]> generator) {
		return instance != null ? instance.toArray(generator) : null;
	}

	private static Collection<String> removeIfClassInMoudleNames(final String[] ss, final String... moduleNames) {
		//
		final List<String> list = testAndApply(Objects::nonNull,
				testAndApply(Objects::nonNull, ss, Arrays::asList, null), ArrayList::new, null);
		//
		for (int i = IterableUtils.size(list) - 1; i >= 0; i--) {
			//
			if (ArrayUtils.contains(moduleNames,
					getName(getModule(forName(replace(IterableUtils.get(list, i), '/', '.')))))) {
				//
				list.remove(i);
				//
			} // if
				//
		} // for
			//
		return list;
		//
	}

	private static <T> void forEach(final Stream<T> instance, final Consumer<? super T> action) {
		if (instance != null) {
			instance.forEach(action);
		}
	}

	private static String toString(final Object instance) {
		return instance != null ? instance.toString() : null;
	}

	private static Class<?> getClass(final Object instance) {
		return instance != null ? instance.getClass() : null;
	}

	private static Module getModule(final Class<?> instance) {
		return instance != null ? instance.getModule() : null;
	}

	private static String getName(final Module instance) {
		return instance != null ? instance.getName() : null;
	}

	private static Class<?> forName(final String instance) {
		try {
			return instance != null ? Class.forName(instance) : null;
		} catch (final ClassNotFoundException e) {
			return null;
		}
	}

	private static class InnerClass {

		private String name, outerName, innerName = null;

	}

	private static class Method {

		private String name, descriptor, signature = null;

		private String[] exceptions = null;

	}

	private static class ClassVisitorImpl extends ClassVisitor {

		private String name, superName = null;

		private String[] interfaces = null;

		private String[] fieldTypes = null;

		private InnerClass[] innerClasses = null;

		private Method[] methods = null;

		private FieldVisitorImpl fvi = null;

		private MethodVisitorImpl mvi = null;

		private ClassVisitorImpl() {
			super(Opcodes.ASM9);
		}

		@Override
		public void visit(final int version, final int access, final String name, final String signature,
				final String superName, final String[] interfaces) {
			//
			this.name = name;
			//
			this.superName = superName;
			//
			this.interfaces = interfaces;
			//
			super.visit(version, access, name, signature, superName, interfaces);
			//
		}

		@Override
		public FieldVisitor visitField(final int access, final String name, final String descriptor,
				final String signature, final Object value) {
			//
			if (descriptor != null && !descriptor.isEmpty() && descriptor.charAt(0) == 'L' && descriptor.length() > 1
					&& descriptor.charAt(StringUtils.length(descriptor) - 1) == ';') {
				//
				final String fieldType = StringUtils.substring(descriptor, 1, StringUtils.length(descriptor) - 1);
				//
				if (!ArrayUtils.contains(fieldTypes, fieldType)) {
					//
					fieldTypes = ArrayUtils.add(fieldTypes, fieldType);
					//
				} // if
					//
			} // if
				//
			if (signature != null) {
				//
				final int index1 = indexOf(signature, "<L");
				//
				final int index2 = indexOf(signature, ";>;");
				//
				if (index2 > index1) {
					//
					final String fieldType = StringUtils.substring(signature, index1 + StringUtils.length("<L"),
							index2);
					//
					if (!ArrayUtils.contains(fieldTypes, fieldType)) {
						//
						fieldTypes = ArrayUtils.add(fieldTypes, fieldType);
						//
					} // if
						//
				} // if
					//
			} // if
				//
			return fvi;
			//
		}

		@Override
		public void visitInnerClass(final String name, final String outerName, final String innerName,
				final int access) {
			//
			if (innerClasses == null) {
				//
				innerClasses = new InnerClass[] {};
				//
			} // if
				//
			InnerClass temp = null;
			//
			boolean found = false;
			//
			for (int i = 0; i < innerClasses.length; i++) {
				//
				if ((temp = ArrayUtils.get(innerClasses, i)) == null) {
					//
					continue;
					//
				} // if
					//
				if (Objects.equals(temp.innerName, innerName) && Objects.equals(temp.name, name)
						&& Objects.equals(temp.outerName, outerName) && (found = true)) {
					//
					break;
					//
				} // if
					//
			} // for
				//
			if (!found) {
				//
				(temp = new InnerClass()).innerName = innerName;
				//
				temp.name = name;
				//
				temp.outerName = outerName;
				//
				innerClasses = ArrayUtils.add(innerClasses, temp);
				//
			} // if
				//
			super.visitInnerClass(name, outerName, innerName, access);
			//
		}

		@Override
		public MethodVisitor visitMethod(final int access, final String name, final String descriptor,
				final String signature, final String[] exceptions) {
			//
			if (methods == null) {
				//
				methods = new Method[] {};
				//
			} // if
				//
			Method temp = null;
			//
			boolean found = false;
			//
			for (int i = 0; i < methods.length; i++) {
				//
				if ((temp = ArrayUtils.get(methods, i)) == null) {
					//
					continue;
					//
				} // if
					//
				if (Objects.equals(temp.descriptor, descriptor) && Arrays.equals(temp.exceptions, exceptions)
						&& Objects.equals(temp.name, name) && Objects.equals(temp.signature, signature)
						&& (found = true)) {
					//
					break;
					//
				} // if
					//
			} // for
				//
			if (!found) {
				//
				(temp = new Method()).descriptor = descriptor;
				//
				temp.exceptions = exceptions;
				//
				temp.name = name;
				//
				temp.signature = signature;
				//
				methods = ArrayUtils.add(methods, temp);
				//
			} // if
				//
				// return super.visitMethod(access, name, descriptor, signature, exceptions);
				//
			return mvi;
			//
		}

	}

	private static class FieldVisitorImpl extends FieldVisitor {

		private Collection<String> classNames = null;

		private FieldVisitorImpl() {
			super(Opcodes.ASM9);
		}

		@Override
		public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
			//
			if (descriptor != null && descriptor.startsWith("L") && descriptor.endsWith(";")) {
				//
				final String substringBetween = StringUtils.substringBetween(descriptor, "L", ";");
				//
				if (!contains(classNames = ObjectUtils.getIfNull(classNames, ArrayList::new), substringBetween)) {
					//
					add(classNames, substringBetween);
					//
				} // if
					//
				return super.visitAnnotation(descriptor, visible);
				//
			} // if
				//
			throw new RuntimeException();
			//
		}

		@Override
		public void visitAttribute(final Attribute attribute) {
			throw new RuntimeException();
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(final int typeRef, final TypePath typePath,
				final String descriptor, final boolean visible) {
			throw new RuntimeException();
		}

	}

	private static class MethodVisitorImpl extends MethodVisitor {

		private static Collection<Type> declaredTypes = null;

		private Collection<String> classNames = null;

		private MethodVisitorImpl() {
			super(Opcodes.ASM9);
		}

		@Override
		public void visitAnnotableParameterCount(final int parameterCount, final boolean visible) {
			throw new RuntimeException();
		}

		@Override
		public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
			throw new RuntimeException();
		}

		@Override
		public AnnotationVisitor visitAnnotationDefault() {
			throw new RuntimeException();
		}

		@Override
		public void visitAttribute(final Attribute attribute) {
			throw new RuntimeException();
		}

		@Override
		public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
			//
			if (!contains(classNames = ObjectUtils.getIfNull(classNames, ArrayList::new), owner)) {
				//
				add(classNames, owner);
				//
			} // if
				//
		}

		@Override
		public AnnotationVisitor visitInsnAnnotation(final int typeRef, final TypePath typePath,
				final String descriptor, final boolean visible) {
			throw new RuntimeException();
		}

		@Override
		public void visitInvokeDynamicInsn(final String name, final String descriptor,
				final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
			//
			if (descriptor != null) {
				//
				if (descriptor.startsWith("()L") && descriptor.endsWith(";")) {
					//
					final String substringBetween = StringUtils.substringBetween(descriptor, "()L", ";");
					//
					if (!contains(classNames = ObjectUtils.getIfNull(classNames, ArrayList::new), substringBetween)) {
						//
						add(classNames, substringBetween);
						//
					} // if
						//
					return;
					//
				} else if (descriptor.startsWith("(L") && descriptor.contains(";)L") && descriptor.endsWith(";")) {
					//
					String substringBetween = StringUtils.substringBetween(descriptor, "(L", ";)L");
					//
					if (!contains(classNames = ObjectUtils.getIfNull(classNames, ArrayList::new), substringBetween)) {
						//
						add(classNames, substringBetween);
						//
					} // if
						//
					if (!contains(classNames = ObjectUtils.getIfNull(classNames, ArrayList::new),
							substringBetween = StringUtils.substringBetween(descriptor, ";)L", ";"))) {
						//
						add(classNames, substringBetween);
						//
					} // if
						//
					return;
					//
				} // if
					//
			} // if
				//
			throw new RuntimeException(descriptor);
			//
		}

		@Override
		public void visitLocalVariable(final String name, final String descriptor, final String signature,
				final Label start, final Label end, final int index) {
			//
			if (descriptor != null) {
				//
				if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
					//
					final String substringBetween = StringUtils.substringBetween(descriptor, "L", ";");
					//
					if (!contains(classNames = ObjectUtils.getIfNull(classNames, ArrayList::new), substringBetween)) {
						//
						add(classNames, substringBetween);
						//
					} // if
						//
					return;
					//
				} else if (descriptor.startsWith("[L") && descriptor.endsWith(";")) {
					//
					final String substringBetween = StringUtils.substringBetween(descriptor, "[L", ";");
					//
					if (!contains(classNames = ObjectUtils.getIfNull(classNames, ArrayList::new), substringBetween)) {
						//
						add(classNames, substringBetween);
						//
					} // if
						//
					return;
					//
				} // if
					//
			} // if
				//
			if (declaredTypes == null) {
				//
				declaredTypes = toList(map(
						filter(stream(testAndApply(Objects::nonNull, Type.class, FieldUtils::getAllFieldsList, null)),
								f -> f != null && Objects.equals(f.getType(), Type.class)
										&& Modifier.isStatic(f.getModifiers())),
						f -> cast(Type.class, Narcissus.getStaticField(f))));
				//
			} // if
				//
			if (contains(declaredTypes, Type.getType(descriptor))) {
				//
				return;
				//
			} // if
				//
			throw new RuntimeException(descriptor);
			//
		}

		@Override
		public AnnotationVisitor visitLocalVariableAnnotation(final int typeRef, final TypePath typePath,
				final Label[] start, final Label[] end, final int[] index, final String descriptor,
				final boolean visible) {
			throw new RuntimeException();
		}

		@Override
		public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
			throw new RuntimeException();
		}

		@Override
		public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor) {
			throw new RuntimeException();
		}

		@Override
		public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor,
				final boolean isInterface) {
			//
			if (!contains(classNames = ObjectUtils.getIfNull(classNames, ArrayList::new), owner)) {
				//
				add(classNames, owner);
				//
			} // if
				//
		}

		@Override
		public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
			throw new RuntimeException();
		}

		@Override
		public void visitParameter(final String name, final int access) {
			throw new RuntimeException();
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(final int parameter, final String descriptor,
				final boolean visible) {
			throw new RuntimeException();
		}

		@Override
		public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
			throw new RuntimeException();
		}

		@Override
		public AnnotationVisitor visitTryCatchAnnotation(final int typeRef, final TypePath typePath,
				final String descriptor, final boolean visible) {
			throw new RuntimeException();
		}

		@Override
		public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
			//
			if (!contains(classNames = ObjectUtils.getIfNull(classNames, ArrayList::new), type)) {
				//
				add(classNames, type);
				//
			} // if
				//
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(final int typeRef, final TypePath typePath,
				final String descriptor, final boolean visible) {
			throw new RuntimeException();
		}

		@Override
		public void visitTypeInsn(final int opcode, final String type) {
			//
			if (!contains(classNames = ObjectUtils.getIfNull(classNames, ArrayList::new), type)) {
				//
				add(classNames, type);
				//
			} // if
				//
		}

	}

	private static int indexOf(final String instance, final String str) {
		return instance != null ? instance.indexOf(str) : -1;
	}

	private static String getMainClass(final ZipFile instance) throws IOException {
		//
		try (final InputStream is = testAndApply(Objects::nonNull,
				instance != null ? instance.getEntry("META-INF/MANIFEST.MF") : null,
				x -> instance != null ? instance.getInputStream(x) : null, null)) {
			//
			final List<String> list = toList(map(
					filter(stream(testAndApply(Objects::nonNull, is, x -> IOUtils.readLines(x, StandardCharsets.UTF_8),
							null)), x -> startsWith(x, "Main-Class: ")),
					x -> StringUtils.substringAfter(Objects.toString(x), "Main-Class: ")));
			//
			if (IterableUtils.size(list) > 1) {
				//
				throw new IllegalStateException();
				//
			} // if
				//
			return testAndApply(x -> IterableUtils.size(x) == 1, list, x -> IterableUtils.get(x, 0), null);
			//
		} // try
	}

	private static String replace(final String instance, final char oldChar, final char newChar) {
		return instance != null ? instance.replace(oldChar, newChar) : null;
	}

	private static <T, R> Stream<R> map(final Stream<T> instance, final Function<? super T, ? extends R> mapper) {
		return instance != null ? instance.map(mapper) : null;
	}

	private static <T> List<T> toList(final Stream<T> instance) {
		return instance != null ? instance.toList() : null;
	}

	private static <T> Stream<T> filter(final Stream<T> instance, final Predicate<? super T> predicate) {
		return instance != null ? instance.filter(predicate) : instance;
	}

	private static boolean startsWith(final String instance, final String prefix) {
		return instance != null && instance.startsWith(prefix);
	}

	private static <T> Stream<T> stream(final Collection<T> instance) {
		return instance != null ? instance.stream() : null;
	}

	private static ContentType getContentType(final ContentInfo instance) {
		return instance != null ? instance.getContentType() : null;
	}

	private static <T, R, E extends Throwable> R testAndApply(final Predicate<T> predicate, final T value,
			final FailableFunction<T, R, E> functionTrue, final FailableFunction<T, R, E> functionFalse) throws E {
		return test(predicate, value) ? apply(functionTrue, value) : apply(functionFalse, value);
	}

	private static <T> boolean test(final Predicate<T> instance, final T value) {
		return instance != null && instance.test(value);
	}

	private static <T, R, E extends Throwable> R apply(final FailableFunction<T, R, E> instance, final T value)
			throws E {
		return instance != null ? instance.apply(value) : null;
	}

}