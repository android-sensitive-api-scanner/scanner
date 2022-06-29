package jadx.api;

import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeNodeRef;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.AnonymousClassAttr;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.ListUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class JavaClass implements JavaNode {
	private static final Logger LOG = LoggerFactory.getLogger(JavaClass.class);

	private final JadxDecompiler decompiler;
	private final ClassNode cls;
	private final JavaClass parent;

	private List<JavaClass> innerClasses = Collections.emptyList();
	private List<JavaClass> inlinedClasses = Collections.emptyList();
	private List<JavaField> fields = Collections.emptyList();
	private List<JavaMethod> methods = Collections.emptyList();
	private boolean listsLoaded;

	JavaClass(ClassNode classNode, JadxDecompiler decompiler) {
		this.decompiler = decompiler;
		this.cls = classNode;
		this.parent = null;
	}

	/**
	 * Inner classes constructor
	 */
	JavaClass(ClassNode classNode, JavaClass parent) {
		this.decompiler = null;
		this.cls = classNode;
		this.parent = parent;
	}

	public String getCode() {
		return getCodeInfo().getCodeStr();
	}

	public @NotNull ICodeInfo getCodeInfo() {
		load();
		return cls.decompile();
	}

	public void decompile() {
		load();
	}

	public synchronized ICodeInfo reload() {
		listsLoaded = false;
		return cls.reloadCode();
	}

	public void unload() {
		listsLoaded = false;
		cls.unloadCode();
	}

	public boolean isNoCode() {
		return cls.contains(AFlag.DONT_GENERATE);
	}

	public boolean isInner() {
		return cls.isInner();
	}

	public synchronized String getSmali() {
		return cls.getDisassembledCode();
	}

	@Override
	public boolean isOwnCodeAnnotation(ICodeAnnotation ann) {
		if (ann.getAnnType() == ICodeAnnotation.AnnType.CLASS) {
			return ann.equals(cls);
		}
		return false;
	}

	/**
	 * Internal API. Not Stable!
	 */
	@ApiStatus.Internal
	public ClassNode getClassNode() {
		return cls;
	}

	/**
	 * Decompile class and loads internal lists of fields, methods, etc.
	 * Do nothing if already loaded.
	 */
	@Nullable
	private synchronized void load() {
		if (listsLoaded) {
			return;
		}
		listsLoaded = true;
		JadxDecompiler rootDecompiler = getRootDecompiler();
		ICodeCache codeCache = rootDecompiler.getArgs().getCodeCache();
		if (!codeCache.contains(cls.getRawName())) {
			cls.decompile();
		}

		int inClsCount = cls.getInnerClasses().size();
		if (inClsCount != 0) {
			List<JavaClass> list = new ArrayList<>(inClsCount);
			for (ClassNode inner : cls.getInnerClasses()) {
				if (!inner.contains(AFlag.DONT_GENERATE)) {
					JavaClass javaClass = rootDecompiler.convertClassNode(inner);
					javaClass.load();
					list.add(javaClass);
				}
			}
			this.innerClasses = Collections.unmodifiableList(list);
		}
		int inlinedClsCount = cls.getInlinedClasses().size();
		if (inlinedClsCount != 0) {
			List<JavaClass> list = new ArrayList<>(inlinedClsCount);
			for (ClassNode inner : cls.getInlinedClasses()) {
				JavaClass javaClass = rootDecompiler.convertClassNode(inner);
				javaClass.load();
				list.add(javaClass);
			}
			this.inlinedClasses = Collections.unmodifiableList(list);
		}

		int fieldsCount = cls.getFields().size();
		if (fieldsCount != 0) {
			List<JavaField> flds = new ArrayList<>(fieldsCount);
			for (FieldNode f : cls.getFields()) {
				if (!f.contains(AFlag.DONT_GENERATE)) {
					flds.add(rootDecompiler.convertFieldNode(f));
				}
			}
			this.fields = Collections.unmodifiableList(flds);
		}

		int methodsCount = cls.getMethods().size();
		if (methodsCount != 0) {
			List<JavaMethod> mths = new ArrayList<>(methodsCount);
			for (MethodNode m : cls.getMethods()) {
				if (!m.contains(AFlag.DONT_GENERATE)) {
					mths.add(rootDecompiler.convertMethodNode(m));
				}
			}
			mths.sort(Comparator.comparing(JavaMethod::getName));
			this.methods = Collections.unmodifiableList(mths);
		}
	}

	JadxDecompiler getRootDecompiler() {
		if (parent != null) {
			return parent.getRootDecompiler();
		}
		return decompiler;
	}

	public ICodeAnnotation getAnnotationAt(int pos) {
		return getCodeInfo().getCodeMetadata().getAt(pos);
	}

	public Map<Integer, JavaNode> getUsageMap() {
		Map<Integer, ICodeAnnotation> map = getCodeInfo().getCodeMetadata().getAsMap();
		if (map.isEmpty() || decompiler == null) {
			return Collections.emptyMap();
		}
		Map<Integer, JavaNode> resultMap = new HashMap<>(map.size());
		for (Map.Entry<Integer, ICodeAnnotation> entry : map.entrySet()) {
			int codePosition = entry.getKey();
			ICodeAnnotation obj = entry.getValue();
			if (obj instanceof ICodeNodeRef) {
				JavaNode node = getRootDecompiler().getJavaNodeByRef((ICodeNodeRef) obj);
				if (node != null) {
					resultMap.put(codePosition, node);
				}
			}
		}
		return resultMap;
	}

	public List<Integer> getUsePlacesFor(ICodeInfo codeInfo, JavaNode javaNode) {
		if (!codeInfo.hasMetadata()) {
			return Collections.emptyList();
		}
		List<Integer> result = new ArrayList<>();
		codeInfo.getCodeMetadata().searchDown(0, (pos, ann) -> {
			if (javaNode.isOwnCodeAnnotation(ann)) {
				result.add(pos);
			}
			return null;
		});
		return result;
	}

	@Override
	public List<JavaNode> getUseIn() {
		return getRootDecompiler().convertNodes(cls.getUseIn());
	}

	public Integer getSourceLine(int decompiledLine) {
		return getCodeInfo().getCodeMetadata().getLineMapping().get(decompiledLine);
	}

	@Override
	public String getName() {
		return cls.getShortName();
	}

	@Override
	public String getFullName() {
		return cls.getFullName();
	}

	public String getRawName() {
		return cls.getRawName();
	}

	public String getPackage() {
		return cls.getPackage();
	}

	@Override
	public JavaClass getDeclaringClass() {
		return parent;
	}

	@Override
	public JavaClass getTopParentClass() {
		if (cls.contains(AType.ANONYMOUS_CLASS)) {
			// moved to usage class
			return getParentForAnonymousClass();
		}
		return parent == null ? this : parent.getTopParentClass();
	}

	private JavaClass getParentForAnonymousClass() {
		AnonymousClassAttr attr = cls.get(AType.ANONYMOUS_CLASS);
		ClassNode topParentClass = attr.getOuterCls().getTopParentClass();
		return getRootDecompiler().convertClassNode(topParentClass);
	}

	public AccessInfo getAccessInfo() {
		return cls.getAccessFlags();
	}

	public List<JavaClass> getInnerClasses() {
		load();
		return innerClasses;
	}

	public List<JavaClass> getInlinedClasses() {
		load();
		return inlinedClasses;
	}

	public List<JavaField> getFields() {
		load();
		return fields;
	}

	public List<JavaMethod> getMethods() {
		load();
		return methods;
	}

	@Nullable
	public JavaMethod searchMethodByShortId(String shortId) {
		MethodNode methodNode = cls.searchMethodByShortId(shortId);
		if (methodNode == null) {
			return null;
		}
		return getRootDecompiler().convertMethodNode(methodNode);
	}

	public List<JavaClass> getDependencies() {
		JadxDecompiler d = getRootDecompiler();
		return ListUtils.map(cls.getDependencies(), d::convertClassNode);
	}

	public int getTotalDepsCount() {
		return cls.getTotalDepsCount();
	}

	@Override
	public void removeAlias() {
		this.cls.getClassInfo().removeAlias();
	}

	@Override
	public int getDefPos() {
		return cls.getDefPosition();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof JavaClass && cls.equals(((JavaClass) o).cls);
	}

	@Override
	public int hashCode() {
		return cls.hashCode();
	}

	@Override
	public String toString() {
		return getFullName();
	}
}
