package com.ast.back.modules.classmgmt.application.impl;

import com.ast.back.modules.assignment.persistence.entity.Assignment;
import com.ast.back.modules.classmgmt.persistence.entity.Clazz;
import com.ast.back.modules.plagiarism.persistence.entity.PlagiarismJob;
import com.ast.back.modules.plagiarism.persistence.entity.SimilarityPair;
import com.ast.back.modules.plagiarism.persistence.mapper.PlagiarismJobMapper;
import com.ast.back.modules.plagiarism.persistence.mapper.SimilarityPairMapper;
import com.ast.back.modules.submission.persistence.entity.Submission;
import com.ast.back.modules.submission.persistence.mapper.SubmissionMapper;
import com.ast.back.modules.user.persistence.entity.User;
import com.ast.back.modules.assignment.persistence.mapper.AssignmentMapper;
import com.ast.back.modules.classmgmt.persistence.mapper.ClassMapper;
import com.ast.back.modules.classmgmt.application.ClassService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.ast.back.modules.classmgmt.persistence.entity.ClassStudent;
import com.ast.back.modules.notice.persistence.entity.Notice;
import com.ast.back.modules.user.persistence.entity.StudentInfo;
import com.ast.back.modules.classmgmt.persistence.mapper.ClassStudentMapper;
import com.ast.back.modules.user.persistence.mapper.UserMapper;
import com.ast.back.modules.notice.application.NoticeService;
import com.ast.back.modules.user.application.StudentInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import java.util.Collections;

@Service
public class ClassServiceImpl extends ServiceImpl<ClassMapper, Clazz> implements ClassService {

    @Autowired
    private ClassStudentMapper classStudentMapper;
    
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AssignmentMapper assignmentMapper;

    @Autowired
    private SubmissionMapper submissionMapper;

    @Autowired
    private PlagiarismJobMapper plagiarismJobMapper;

    @Autowired
    private SimilarityPairMapper similarityPairMapper;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private StudentInfoService studentInfoService;

    @Override
    public List<Map<String, Object>> getStudentsByClassId(Integer classId, Long teacherId) {
        return baseMapper.selectStudentsByClassId(classId, teacherId);
    }

    @Override
    public boolean removeStudentFromClass(Integer classId, Long studentId, Long teacherId) {
        return baseMapper.deleteStudentFromClass(classId, studentId, teacherId) > 0;
    }

    @Override
    public boolean createNewClass(Clazz clazz) {
        // 1. 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸婂潡鏌ㄩ弴鐐测偓褰掑磿閹寸姵鍠愰柣妤€鐗嗙粭鎺楁煕閵娿儱鈧悂鍩為幋锔藉亹閻庡湱濮撮ˉ婵堢磽娴ｇ懓濮堟い銊ワ躬瀵鎮㈤崗鐓庝罕闂佸壊鍋嗛崰鎾诲礄閿熺姵鈷戦柟鑲╁仜婵″ジ鏌涙繝鍌滅Ш妤犵偛鐗撴俊鎼佸Ψ鎼达絽鏋涙鐐村姈閹棃鏁愰崘锝呬壕闁规壆澧楅埛鎴︽煕韫囨艾浜归柟钘夊暞閵囧嫰顢曢姀鈺傗枅婵犵绱曢弫璇茬暦閻旂⒈鏁嶆慨姗嗗墮閺併倝姊绘笟鈧褑鍣归梺鍛婃处閸嬪懘鎮甸灏栨斀闁挎稑瀚禍濂告煕婵犲啯绀堥柟骞垮灩铻ｉ柤濮愬€ゅú鎼佹煟閻樼儤銆冮悹鈧敃鈧…鍥冀閵娧咁啎閻庣懓澹婇崰鏇㈠箟妤ｅ啯鐓涘ù锝呮啞婢跺嫮绱掔紒妯兼创妤犵偛顑夐幃妯兼嫚閹绘帗鍊紓浣规⒒閸犳牕顕ｉ幘顔碱潊闁抽敮鍋撻柟閿嬫そ濮婃椽鎮℃惔顔界稐闂佺锕ュú鏍偤椤撶喓绡€闁汇垽娼ф禒婊勪繆椤栨熬韬€规洩缍佸畷姗€顢橀悤鍌滅＝闂傚倸鍊烽懗鍫曞箠閹惧瓨娅犻柣锝呰嫰閸ㄦ繃銇勯弽顐粶缁?
        String inviteCode = generateUniqueInviteCode();
        clazz.setInviteCode(inviteCode);
        
        // 2. 闂傚倸鍊搁崐鎼佸磹瀹勬噴褰掑炊瑜忛弳锕傛煕椤垵浜濋柛娆忕箳閳ь剝顫夊ú鏍洪妸鈺傚剹闁糕剝顦鸿ぐ鎺撴櫜闁割偒鍋呯紞鍫濃攽閻愬弶鍣藉┑顔芥尦濠€渚€姊虹紒妯忣亜螣婵犲洤纾块柟鎵閻撴洟鎮楅敐鍛倎闂侇収鍨堕弻锛勪沪閸撗勫垱闂佺硶鏅涚€氭澘鐣峰鈧、鏃€鎷呯拠鈩冪秾缂傚倸鍊搁崐椋庢媼閺屻儱纾婚柟鍓х帛閻撴洟鏌嶉埡浣告灓闁绘帞鏅槐鎺楁偐閼姐倗鏆梺鍝勭灱閸犳捇鍩€椤掑倹鏆╂い顓炵墦瀹曟粓鏌嗗鍡欏幗闂佸湱鍎ゅ鐟扳枍閺囩姷纾奸弶鍫涘妼濞搭喗顨ラ悙宸Ш闁逞屽墾缂嶅棝宕滃☉銏犳辈闁靛牆顦伴埛鎴︽煕濠靛棗顏柣鎺曟硶缁辨帞绱掑Ο鑲╃暤闂佷紮绲介崲鏌ワ綖濠婂牆鐒垫い鎺嶆缁诲棙鎱ㄥ┑鍡欑劸婵℃彃顭峰铏圭磼濡鏆楅梺鍝ュТ闁帮綁宕?
        clazz.setCreateTime(LocalDateTime.now());
        
        // 3. 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗霉閿濆浜ら柤鏉挎健瀵爼宕煎顓熺彅闂佹悶鍔嶇换鍐Φ閸曨垰鍐€闁靛ě鍛帒闂備礁鎼Λ娆戝垝閹捐钃熼柣鏂垮悑閸婄粯鎱ㄥΔ鈧Λ娆撴偩閸洘鈷戞繛鑼额嚙楠炴銇勯妸銉含鐎殿喛顕ч埥澶愬閻橀潧濮堕梻浣告啞閸旓附绂嶉弽顬綁宕奸妷锔规嫼闂佸憡绻傜€氱兘宕曟惔锝囩＜闁兼悂娼ч崫铏光偓娈垮枛椤兘寮幇鏉垮窛闁稿本绋掗ˉ鍫ユ煕閳规儳浜炬俊鐐€栫敮鎺斺偓姘煎弮瀹曟垹鈧綆鍠楅悡鏇㈡煃閳轰礁鏆熼柟鍐叉嚇閺岋綁顢橀悙闈涒叺闂佸搫琚崝鎴濐嚕閹绢喗鍊锋繛鏉戭儏娴滈箖鏌涘┑鍕姢濞?
        return this.save(clazz);
    }

    @Override
    public IPage<Clazz> getClassesByTeacherId(Long teacherId, int page, int size) {
        Page<Clazz> p = new Page<>(page, size);
        return baseMapper.selectClassesWithStudentCount(p, teacherId);
    }

    @Override
    public Map<String, Object> getTeacherStats(Long teacherId) {
        List<Clazz> teacherClasses = this.list(new LambdaQueryWrapper<Clazz>()
                .eq(Clazz::getTeacherId, teacherId)
                .select(Clazz::getId, Clazz::getClassName));
        long classCount = teacherClasses.size();
        List<Integer> classIds = teacherClasses.stream().map(Clazz::getId).collect(Collectors.toList());
        Map<Integer, String> classNameMap = teacherClasses.stream()
                .collect(Collectors.toMap(Clazz::getId, Clazz::getClassName));

        Long studentCount = baseMapper.countStudentsByTeacherId(teacherId);

        List<Assignment> assignments = Collections.emptyList();
        if (!classIds.isEmpty()) {
            try {
                assignments = assignmentMapper.selectList(new LambdaQueryWrapper<Assignment>()
                        .in(Assignment::getClazzId, classIds)
                        .orderByAsc(Assignment::getDeadline));
            } catch (BadSqlGrammarException exception) {
                assignments = Collections.emptyList();
            }
        }
        long homeworkCount = assignments.size();

        List<Map<String, Object>> distribution = baseMapper.selectClassStudentDistribution(teacherId);
        Map<Integer, Long> classStudentCountMap = new HashMap<>();
        List<String> names = new ArrayList<>();
        List<Long> counts = new ArrayList<>();

        if (distribution != null) {
            for (Map<String, Object> item : distribution) {
                String className = String.valueOf(item.getOrDefault("name", "未命名班级"));
                Number studentCountValue = (Number) (item.containsKey("studentCount")
                        ? item.get("studentCount")
                        : item.get("student_count"));
                long classStudentCount = studentCountValue == null ? 0L : studentCountValue.longValue();

                names.add(className);
                counts.add(classStudentCount);

                teacherClasses.stream()
                        .filter(clazz -> className.equals(clazz.getClassName()))
                        .findFirst()
                        .ifPresent(clazz -> classStudentCountMap.put(clazz.getId(), classStudentCount));
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfToday = now.toLocalDate().plusDays(1).atStartOfDay();
        LocalDateTime endOfThreeDays = now.plusDays(3);
        LocalDateTime endOfSevenDays = now.plusDays(7);

        long todayDeadlineCount = assignments.stream()
                .map(this::resolveDeadline)
                .filter(deadline -> deadline != null && !deadline.isBefore(now) && deadline.isBefore(endOfToday))
                .count();

        long threeDaysDeadlineCount = assignments.stream()
                .map(this::resolveDeadline)
                .filter(deadline -> deadline != null && !deadline.isBefore(now) && !deadline.isAfter(endOfThreeDays))
                .count();

        long sevenDaysDeadlineCount = assignments.stream()
                .map(this::resolveDeadline)
                .filter(deadline -> deadline != null && !deadline.isBefore(now) && !deadline.isAfter(endOfSevenDays))
                .count();

        List<Map<String, Object>> recentDeadlines = assignments.stream()
                .filter(item -> resolveDeadline(item) != null && !resolveDeadline(item).isBefore(now))
                .limit(5)
                .map(item -> {
                    Map<String, Object> recent = new HashMap<>();
                    recent.put("id", item.getId());
                    recent.put("title", item.getTitle());
                    recent.put("className", classNameMap.getOrDefault(item.getClazzId(), "Unassigned Class"));
                    recent.put("deadline", resolveDeadline(item));
                    return recent;
                })
                .collect(Collectors.toList());

        List<Long> assignmentIds = assignments.stream()
                .map(Assignment::getId)
                .collect(Collectors.toList());

        List<Submission> latestSubmissions = Collections.emptyList();
        List<PlagiarismJob> plagiarismJobs = Collections.emptyList();
        List<SimilarityPair> similarityPairs = Collections.emptyList();

        if (!assignmentIds.isEmpty()) {
            try {
                latestSubmissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                        .in(Submission::getAssignmentId, assignmentIds)
                        .eq(Submission::getIsLatest, 1));
            } catch (BadSqlGrammarException exception) {
                latestSubmissions = Collections.emptyList();
            }

            try {
                plagiarismJobs = plagiarismJobMapper.selectList(new LambdaQueryWrapper<PlagiarismJob>()
                        .in(PlagiarismJob::getAssignmentId, assignmentIds));
            } catch (BadSqlGrammarException exception) {
                plagiarismJobs = Collections.emptyList();
            }
        }

        if (!plagiarismJobs.isEmpty()) {
            List<Long> jobIds = plagiarismJobs.stream()
                    .map(PlagiarismJob::getId)
                    .collect(Collectors.toList());
            try {
                similarityPairs = similarityPairMapper.selectList(new LambdaQueryWrapper<SimilarityPair>()
                        .in(SimilarityPair::getJobId, jobIds));
            } catch (BadSqlGrammarException exception) {
                similarityPairs = Collections.emptyList();
            }
        }

        Map<Integer, Set<Long>> submittedStudentsByClass = new HashMap<>();
        for (Submission submission : latestSubmissions) {
            Integer classId = submission.getClassId();
            Long studentId = submission.getStudentId();
            if (classId == null || studentId == null) {
                continue;
            }
            submittedStudentsByClass
                    .computeIfAbsent(classId, key -> new HashSet<>())
                    .add(studentId);
        }

        List<Map<String, Object>> activityChart = teacherClasses.stream()
                .map(clazz -> {
                    long totalStudents = classStudentCountMap.getOrDefault(clazz.getId(), 0L);
                    long submittedStudents = submittedStudentsByClass.getOrDefault(clazz.getId(), Collections.emptySet()).size();
                    long pendingStudents = Math.max(totalStudents - submittedStudents, 0L);

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("classId", clazz.getId());
                    item.put("className", clazz.getClassName());
                    item.put("studentCount", totalStudents);
                    item.put("submittedCount", submittedStudents);
                    item.put("pendingCount", pendingStudents);
                    return item;
                })
                .sorted(Comparator.comparingLong(item -> -((Number) item.get("studentCount")).longValue()))
                .limit(6)
                .collect(Collectors.toList());

        Map<String, Long> deadlineTrendMap = new LinkedHashMap<>();
        for (int index = 0; index < 7; index++) {
            LocalDateTime dayStart = now.toLocalDate().plusDays(index).atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);
            long count = assignments.stream()
                    .map(this::resolveDeadline)
                    .filter(deadline -> deadline != null && !deadline.isBefore(dayStart) && deadline.isBefore(dayEnd))
                    .count();
            deadlineTrendMap.put(String.format("%02d/%02d", dayStart.getMonthValue(), dayStart.getDayOfMonth()), count);
        }

        long activeHomeworkCount = assignments.stream()
                .filter(item -> {
                    LocalDateTime deadline = resolveDeadline(item);
                    LocalDateTime startAt = item.getStartAt();
                    boolean started = startAt == null || !startAt.isAfter(now);
                    boolean notEnded = deadline == null || !deadline.isBefore(now);
                    return started && notEnded;
                })
                .count();

        long plagiarismPendingCount = similarityPairs.stream()
                .filter(item -> "PENDING".equalsIgnoreCase(item.getStatus()))
                .count();
        long plagiarismConfirmedCount = similarityPairs.stream()
                .filter(item -> "CONFIRMED".equalsIgnoreCase(item.getStatus()))
                .count();
        long plagiarismFalsePositiveCount = similarityPairs.stream()
                .filter(item -> "FALSE_POSITIVE".equalsIgnoreCase(item.getStatus()))
                .count();

        long warningCount = todayDeadlineCount + plagiarismPendingCount;

        Map<String, Object> stats = new HashMap<>();
        stats.put("classCount", classCount);
        stats.put("studentCount", studentCount != null ? studentCount : 0);
        stats.put("homeworkCount", homeworkCount);
        stats.put("activeHomeworkCount", activeHomeworkCount);
        stats.put("warningCount", warningCount);
        stats.put("chartNames", names);
        stats.put("chartValues", counts);
        stats.put("activityChart", activityChart);
        stats.put("deadlineTrend", deadlineTrendMap.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("label", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList()));
        stats.put("plagiarismDistribution", Map.of(
                "pending", plagiarismPendingCount,
                "confirmed", plagiarismConfirmedCount,
                "falsePositive", plagiarismFalsePositiveCount
        ));
        stats.put("deadlineStats", Map.of(
                "today", todayDeadlineCount,
                "threeDays", threeDaysDeadlineCount,
                "sevenDays", sevenDaysDeadlineCount
        ));
        stats.put("recentDeadlines", recentDeadlines);
        return stats;
    }

    private LocalDateTime resolveDeadline(Assignment assignment) {
        if (assignment == null) {
            return null;
        }
        return assignment.getEndAt() != null ? assignment.getEndAt() : assignment.getDeadline();
    }

    @Override
    public List<Map<String, Object>> getPendingApplications(Long teacherId) {
        // 1. 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕閻庤娲忛崕鎶藉焵椤掑﹦绉甸柛鐘崇墱婢规洟宕稿Δ浣哄幍闂佽鍨虫晶妤吽夋径鎰闁哄鍩婇煬顒勬煛鐏炶鈧繈骞婂┑瀣妞ゆ棁鍋愭晶顖氣攽閻愯尙鎽犵紒顔肩灱缁辩偞绻濋崶褑鎽曞┑鐐村灟閸ㄧ懓鏁俊鐐€栧濠氬储瑜旈敐鐐侯敂閸啿鎷洪梺纭呭亹閸嬫盯鍩€椤掍礁濮嶇€规洘鍨块獮姗€寮妷锔芥澑闂備胶绮崝姗€宕洪弽顑句汗鐟滃繒妲愰幒妤冨彄妞ゆ挾濮烽悡鎾澄旈悩闈涗沪妞ゃ劌鎳橀垾锕傚Ω閳轰線鍞堕梺缁樻濞撳綊鎮芥繝姘拺闁荤喐婢橀幃鎴︽煟閿濆簼閭€规洘绻傞鍏煎緞鐎ｎ亙绨甸梻浣虹帛閺屻劑宕ョ€ｎ喗鍋傞煫鍥ㄦ尨閺€浠嬫煟閹邦垰鐨哄ù鐘灲閺屾盯寮埀顒傚枈瀹ュ桅闁告洦鍠氶悿鈧梺鎸庣箓鐎氼噣鎯勬惔鈽嗘富闁靛牆楠稿銊╂煕鎼粹槅鐓奸挊婵嬫煠绾板崬澧扮痪鍓у帶椤法鎹勯悜妯绘嫳闂佹悶鍊愰崑鎾翠繆閻愵亜鈧牕煤閳哄啰绀婂ù锝呮憸閺嗭妇鎲搁悧鍫濅刊闁轰礁鍟撮弻鏇＄疀鐎ｎ亞浼勯梺缁樹緱閸ｏ絽顫忛搹瑙勫珰闁肩⒈鍓涢濠囨⒑缁嬫鍎戦柛鐘崇墪椤曪綁寮婚妷顔芥櫇闂佹寧娲嶉崑鎾绘煕閹寸姴鈻堥柡灞剧〒娴狅箓宕滆閻撳倻绱掗悙顒€绀冩俊顐㈠濠€渚€姊洪幐搴ｇ畵妞わ箒浜Σ鎰版偋閸粎绠氶梺璇″瀻閸曨偅娈樼紓鍌欒兌缁垳鎹㈤崼銉ユ槬闁逞屽墯閵囧嫰骞掑鍫濆帯缂?
        List<Clazz> classes = this.list(new LambdaQueryWrapper<Clazz>()
                .eq(Clazz::getTeacherId, teacherId)
                .select(Clazz::getId, Clazz::getClassName));
        
        if (classes.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Integer> classIds = classes.stream().map(Clazz::getId).collect(Collectors.toList());
        Map<Integer, String> classMap = classes.stream().collect(Collectors.toMap(Clazz::getId, Clazz::getClassName));

        // 2. 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋為悧鐘汇€侀弴銏犖ч柛鈩冦仦缁剝淇婇悙顏勨偓鏍礉瑜忕划濠氬箣閻樺樊妫滈梺绉嗗嫷娈曢柣鎾寸懅缁辨挻鎷呴棃娑氫患濠电偛鎳忛敃銏ゅ蓟閳╁啯濯撮柛鎾村絻閸撹鲸绻涢敐鍛悙闁挎洦浜濇穱濠囧醇閺囩偟锛滃┑顔斤供閸嬪嫰藟濮橆兘鏀介柨娑樺娴滃ジ鏌涙繝鍐ⅹ閻撱倝鐓崶銊р槈缁炬儳娼″濠氬醇閻旀亽鈧帡鏌￠崱顓犵暤闁哄本娲樼换娑㈡倷椤掍胶褰熼梻浣芥〃缁€浣该洪妸鈺佺疅缂佸顑欓崥瀣熆鐠虹尨鍔熼柣锝囧劋缁绘盯骞橀弶鎴犲姲闂佺顑嗛幐濠氬箞閵婏妇绡€闁搞儮鏅濈粣妤呮⒑闂堟稒澶勯柛鏃€鐟╅悰顕€骞掑Δ鈧粻濠氭煙缂併垹鏋熼柡鍡樏埞鎴︽偐閸偅姣勬繝娈垮櫘閸欏啫鐣烽幋锕€绠荤紓浣诡焽閸樺崬鈹戞幊閸婃洟宕锝囶洸鐟滅増甯楅悡娑㈡倵閿濆骸浜濇い銉ョ墦閺屸€崇暆閳ь剟宕伴幇顒夌劷闊洦鏌ｉ崑鍛存煕閹般劍娅撻柍褜鍓欑粔鐟邦潖閾忓湱鐭欐繛鍡樺劤閸擃參姊洪崨濠冣拹闁搞劌娼￠悰顕€宕橀妸搴㈡瀹曘劑顢橀悢椋庛偠濠碉紕鍋戦崐鏍箰妤ｅ啫纾块柟鐗堟緲閻掑灚銇勯幒鍡椾壕闂佹椿鍘奸崐鍨嚕?(status = 0)
        List<ClassStudent> applications = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>()
                        .in(ClassStudent::getClassId, classIds)
                        .eq(ClassStudent::getStatus, 0)
        );
        
        if (applications.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕閻庤娲忛崕鎶藉焵椤掑﹦绉甸柛鐘崇墱婢规洟宕稿Δ浣哄幍闂佽鍨虫晶妤吽夋径鎰闁哄鍩婇煬顒勬煛鐏炶鈧繈骞婂┑瀣妞ゆ棁鍋愭晶顖氣攽閻愯尙鎽犵紒顔肩灱缁辩偞绻濋崶褑鎽曞┑鐐村灟閸ㄧ懓鏁梻渚€娼х换鍡涘礈濠靛棭鐔嗛柟鐑橆殕閳锋垹绱掔€ｎ偒鍎ラ柛搴㈠姍閺岀喖宕ㄦ繝鍐ㄢ偓鎰版煕閳瑰灝鍔滅€垫澘瀚伴獮鍥敆閸屻倖袨闂佽娴烽幊鎾寸珶婵犲洤绐楁俊銈呮噹缁愭鎱ㄥ璇蹭壕濠殿喖锕︾划顖炲箯閸涘瓨鍤嶉柕澶涚岛閸嬫捇宕橀鐣屽幗闂佺娅ｉ崑鐔兼偩閸偒娈介柣鎰綑婵秹鏌熺粵鍦瘈濠碘€崇埣瀹曞爼鍩￠崘銊ョ疀闂傚倸鍊搁崐椋庢閿熺姴绀堟繛鍡樺灩閻捇鏌熺紒銏犳灍闁稿﹥瀵х换娑橆啅椤旇崵鍑归梺缁樻尰濞茬喖寮婚悢鐓庣骇闁兼剚鍨伴顓㈡⒑绾懏鐝柛鏃€鐟ㄥΛ?
        List<Long> studentIds = applications.stream().map(ClassStudent::getStudentId).distinct().collect(Collectors.toList());
        List<User> students = userMapper.selectBatchIds(studentIds);
        Map<Long, User> studentMap = students.stream().collect(Collectors.toMap(User::getId, u -> u));

        // 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕閻庤娲忛崕鎶藉焵椤掑﹦绉甸柛鐘崇墱婢规洟宕稿Δ浣哄幍闂佽鍨虫晶妤吽夋径鎰闁哄鍩婇煬顒勬煛鐏炶鈧繈骞婂┑瀣妞ゆ棁鍋愭晶顖氣攽閻愯尙鎽犵紒顔肩灱缁辩偞绻濋崶褑鎽曞┑鐐村灟閸ㄧ懓鏁梻渚€娼х换鍡涘礈濠靛鍋熼柡鍥ュ灪閻撶喖骞栧ǎ顒€鐏い锝堝皺閳ь剙鍘滈崑鎾绘煙闂傚顦﹂柦鍐枛閺屾洘绻涢悙顒佺彅濠碘槅鍋呴敃銏ゅ蓟濞戔懇鈧箓骞嬪┑鍥╀簮闂備礁婀遍…鍫ュ疮閺夋埈娼栭柛婵嗗▕閾忚瀚氶柟缁樺笧椤旀劕鈹戞幊閸婃鎱ㄩ悽鍛婂仱闁哄毝棰佹睏婵炴挻鍩冮崑鎾存叏婵犲啯銇濇鐐寸墵閹瑩骞撻幒鎳躲儳绱撻崒娆愮グ閻忓浚浜濋幈銊╁Χ婢跺﹦鏌ч梺鍓插亝濞叉牜绮婚悽鍛婄厵?
        List<StudentInfo> studentInfos = studentInfoService.list(new LambdaQueryWrapper<StudentInfo>().in(StudentInfo::getUserId, studentIds));
        Map<Long, StudentInfo> studentInfoMap = studentInfos.stream().collect(Collectors.toMap(StudentInfo::getUserId, s -> s));

        // 4. 缂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾惧綊鏌熼梻瀵割槮缁惧墽鎳撻—鍐偓锝庝簼閹癸綁鏌ｉ鐐搭棞闂囧鏌ㄥ┑鍡欏闁逞屽厴閸嬫捇姊虹粙娆惧剱闁瑰憡鎮傞敐鐐测攽鐎ｎ偄浜楅柟鑹版彧缁茬厧顩奸幘缁樷拺闁兼亽鍎洪悞浠嬫煕閻樻煡鍙勯柛鈺冨仱楠炲鏁傞挊澶夋睏闂備礁鎲￠悷銉┧囨导鏉戠畺婵炲棙鎸婚埛鎴︽煕濠靛棗顏撮柛搴℃捣缁辨帡鎳滈棃娑樻懙閻庢鍠栭…鐑藉极閹版澘宸濋柛灞剧⊕椤ュ牓鏌涢埞鎯т壕婵＄偑鍊栫敮鎺斺偓姘煎弮瀹曟垹鈧綆鍠楅悡鏇㈡煃閳轰礁鏆熼柟鍐叉嚇閺?
        List<Map<String, Object>> result = new ArrayList<>();
        for (ClassStudent app : applications) {
            User student = studentMap.get(app.getStudentId());
            if (student != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", app.getId()); // 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸婂潡鏌ㄩ弴鐐测偓褰掑磿閹寸姵鍠愰柣妤€鐗嗙粭鎺楁煕閵娿儱鈧骞夐幖浣瑰亱闁割偅绻勯悷鏌ユ⒑缁嬫鍎涘ù婊庝邯瀵鈽夐姀鐘栥劍銇勯弮鍌氬付闁抽攱甯″鐑樻姜閹殿噮妲銈嗗灥濡繃淇婇悽绋跨妞ゆ牗姘ㄩ濠囨⒑閻熸壆鎽犵紒娲畺瀹曟垿骞橀懜闈涙瀭闂佸憡娲﹂崑鍕倶娴ｅ湱绡€闂傚牊绋戦埀顒佹倐楠炲鏁撻悩鍐蹭簵濠电偞鍨堕敃鈺呮偄閸℃稒鍋ｉ柛婵嗗閹牊銇勯幒瀣伄闁汇儺浜、姗€鎮欏顔芥缂傚倷娴囨ご鍝ユ暜濡も偓椤曘儵宕熼銈嗘畷闂侀€炲苯澧存い銏℃⒒缁?
                map.put("studentId", student.getId());
                map.put("username", student.getUsername());
                map.put("nickname", student.getNickname());
                map.put("email", student.getEmail());
                map.put("role", student.getRole());
                
                // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柛娑橈功缁犻箖鏌嶈閸撴氨鎹㈠☉娆愬闁告劕寮剁涵浠嬫煕閵夛絽濡挎い鈺冨厴閹鏁愭惔婵堟晼闂佹寧绋掗惄顖氼潖濞差亜宸濆┑鐘插暙椤︹晠姊洪幖鐐插闁告鍟块悾鐑藉箳濡も偓鎯熼梺鍐叉惈閸婂宕㈤幖浣瑰€垫鐐茬仢閸旀艾螖閻樺弶鍟炵紒鍌氱Ч楠炲洭鎮ч崼銏犲箞闂佽鍑界紞鍡涘磻閸涱垯鐒婃い鎾卞灪閻撳啴鏌﹀Ο渚▓婵☆垪鍋撻柣搴ゎ潐濞叉﹢鏁嬪銈嗘煥缁绘﹢銆佸▎鎾村癄濠㈣泛顑囨禍鐑芥⒒閸屾艾鈧悂宕愬畡鎳婂綊宕惰濞存牠鏌曟繛鐐珕闁稿﹤娼￠弻銊╁即閻愭祴鍋撻崫銉т笉闁哄顕抽弮鍫熸櫜闁告侗鍘藉▓顓犵磽娴ｅ搫顎岄柛銊ョ埣瀵鏁愰崨鍌滃枛閹虫牠鍩℃担渚仹闂傚倷绀侀幖顐﹀箠韫囨洖鍨濋柟鎹愵嚙閽冪喐绻涢幋娆忕仼缂佺姷绮穱濠囧Χ閸屾矮澹曢梻浣侯攰濞呮洟宕濆▎蹇曟殾闁靛繈鍊曞Λ姗€骞栫€涙ɑ灏伴柡鍌楀亾闂傚倷鑳剁划顖炲垂闂堟耽娲Ω閳轰胶顦梺缁橆殔閻楀繒绮绘ィ鍐╃厾缁炬澘宕晶顕€鏌嶇拠鑼ⅱ闁逞屽墲椤煤濡厧鍨濈€光偓閸曨偆鍘撮梺纭呮彧缁犳垿鎮欐繝鍕枑婵犲﹤鐗嗛崥?
                StudentInfo info = studentInfoMap.get(student.getId());
                if (info != null) {
                    map.put("school", info.getSchool());
                    map.put("studentNumber", info.getStudentNumber());
                    map.put("college", info.getCollege());
                    map.put("adminClass", info.getClassName());
                } else {
                    map.put("school", "-");
                    map.put("studentNumber", "-");
                    map.put("college", "-");
                    map.put("adminClass", "-");
                }

                map.put("className", classMap.get(app.getClassId()));
                map.put("applyTime", app.getJoinTime());
                result.add(map);
            }
        }
        
        return result;
    }

    @Override
    public boolean approveApplication(Integer applicationId) {
        ClassStudent app = classStudentMapper.selectById(applicationId);
        if (app != null) {
            app.setStatus(1); // Approved
            int rows = classStudentMapper.updateById(app);
            if (rows > 0) {
                // 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极閹剧粯鍋愰柛鎰紦閻㈠姊绘担鐟邦嚋婵炲弶鐗犲畷鎰板箹娴ｇ鐎梺绋跨灱閸嬬偤鎮￠弴鐘冲枑閹兼番鍔婇埀顒€鍟村畷銊р偓娑櫭埀顒€娼￠弻娑⑩€﹂幋婵呯按婵炲瓨绮嶇划鎾诲蓟閻斿憡缍囬柛鎾楀惙鎴犵磼缂併垹骞栧褍娴峰Σ鎰板箳濡も偓缁犳稒銇勯幘璺烘珡婵☆偄鍟村娲传閸曨喖顏紓浣割槺閸忔鐦繛鎾村焹閸嬫捇鏌＄仦鍓ф创妤犵偞顭囨竟鏇犫偓锝庝憾濡喐淇婇悙顏勨偓銈夊磻閸涱垱宕查柛顐ゅ枍缁诲棝鏌熼梻瀵割槮闁绘挻鐩弻娑氫沪閸撗呭彎缂?
                Clazz clazz = this.getById(app.getClassId());
                if (clazz != null) {
                    Notice notice = new Notice();
                    notice.setTitle("Class joined successfully");
                    notice.setContent("You have joined class: " + clazz.getClassName());
                    notice.setReceiverId(app.getStudentId());
                    notice.setIsRead(0);
                    notice.setType("SYSTEM");
                    notice.setCreateTime(LocalDateTime.now());
                    noticeService.save(notice);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean rejectApplication(Integer applicationId) {
        ClassStudent app = classStudentMapper.selectById(applicationId);
        if (app != null) {
            Clazz clazz = this.getById(app.getClassId());
            int rows = classStudentMapper.deleteById(applicationId);
            if (rows > 0) {
                // 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极閹剧粯鍋愰柛鎰紦閻㈠姊绘担鐟邦嚋婵炲弶鐗犲畷鎰板箹娴ｇ鐎梺绋跨灱閸嬬偤鎮￠弴鐘冲枑閹兼番鍔婇埀顒€鍟村畷銊р偓娑櫭埀顒€娼￠弻娑⑩€﹂幋婵呯按婵炲瓨绮嶇划鎾诲蓟閻斿憡缍囬柛鎾楀惙鎴犵磼缂併垹骞栧褍娴峰Σ鎰板箳濡も偓缁犳稒銇勯幘璺烘珡婵☆偄鍟村娲传閸曨喖顏紓浣割槺閸忔鐦繛鎾村焹閸嬫捇鏌＄仦鍓ф创妤犵偞顭囨竟鏇犫偓锝庝憾濡喐淇婇悙顏勨偓銈夊磻閸涱垱宕查柛顐ゅ枍缁诲棝鏌熼梻瀵割槮闁绘挻鐩弻娑氫沪閸撗呭彎缂?
                if (clazz != null) {
                    Notice notice = new Notice();
                    notice.setTitle("Class join request rejected");
                    notice.setContent("Your request to join class " + clazz.getClassName() + " was rejected.");
                    notice.setReceiverId(app.getStudentId());
                    notice.setIsRead(0);
                    notice.setType("SYSTEM");
                    notice.setCreateTime(LocalDateTime.now());
                    noticeService.save(notice);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean deleteClass(Integer id) {
        // 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极閹剧粯鍋愰柛鎰紦閻㈢粯淇婇悙顏勨偓鏍偋濠婂牆纾绘繛鎴欏灩閸ㄥ倿鏌涚仦鍓р棨濞存粍绮撻弻鈥愁吋閸愩劌顬嬮梺闈涙鐢濡甸崟顖氱厸闁稿本绮屽銊╂⒑閸濆嫮鐏遍柛鐘崇墵楠炲啫顭ㄩ崘鐐缓闂佺硶鍓濋…鍥汲閵夆晜鈷掑ù锝呮啞鐠愶繝鏌熼搹顐ｅ磳闁诡喓鍎茬缓鐣岀矙閼愁垱鎲伴梻浣告惈濞层劑宕伴幘鍓佷笉濡わ絽鍟悡鏇熴亜椤撶喎鐏ラ柡瀣⊕閵囧嫰顢楅埀顒勵敄閸モ晜顫曢柟鎯х摠婵挳鏌涘┑鍡楃彅闁靛繈鍨荤壕鐣屸偓骞垮劚閹锋垵顔忛妷锔轰簻妞ゆ劑鍨荤粻浼存偂閵堝棎浜滈煫鍥ㄦ尭椤忣偅銇勯弬璺ㄧ劯婵﹥妞藉畷鐑筋敇閻愭彃顬嗛梻浣告贡椤牓鈥﹂悜鐣屽祦闊洦绋掗悞鑲┾偓骞垮劚濡矂骞忛搹鍦＝濞达絽澹婂Σ鍛婁繆椤愶綆娈滅€规洘婢橀埥澶愬閵忕姴绲炬俊鐐€栫敮鎺楀磹閻㈢纾婚柟鎹愬煐閸犲棝鏌涢弴銊ュ妞わ负鍔庣槐鎾存媴缁涘娈梺缁橆殕閹瑰洭鎮伴鈧畷姗€顢欓懖鈺佸箰濠电偟顥愭竟鍫㈠垝閹€鏋嶉柨婵嗘处椤洟鏌熼悜妯烘闁绘柨鍚嬮崵鎺楁煏閸繃宸濋柛濠呮珪缁绘繈鎮介棃娴躲儵鏌℃担鍛婂暈闁逛究鍔嶇€靛ジ寮堕幋婵堚偓顓熺節闂堟稑鈧鈥﹂崼銉ュ強闁靛鏅滈悡娑氣偓骞垮劚妤犳悂鐛Δ鍛厱閻庯綆浜堕崕鎰磼缂佹绠栫紒缁樼箞瀹曟帒顫濋鐔烘澒闂傚倷绀侀幖顐︻敄閸℃あ娑㈠礃椤旇壈鎽曢梺缁樻煥閹诧紕绱為崶顬棃鏁愰崨顓熸濡炪倕楠哥粔鐟邦潖閾忓湱纾兼俊顖濇閻熴劌顪冮妶搴″箻闁稿繑锚椤曪絾绻濆顓熸闂佺粯顭堢亸娆撍囬弶娆炬富闁靛牆妫楁慨鍌炴煕婵犲喚娈滄い銏＄懄缁绘繈宕堕妸褍骞楅梻渚€娼х换鍡涘疾濞戙垺鍊堕柣妯哄帠缁诲棙銇勯幇鍓佹偧缂佺姷鍋熼埀顒侇問閸犳牠鈥﹂柨瀣╃箚闁归棿绀佸敮闂侀潧绻嗗Σ鍛焽閺冣偓缁绘繄鍠婂Ο娲绘綉闂佹悶鍔岀壕顓㈠礆閹烘鏁嶉柣鎰皺椤斿棝姊绘笟鍥у缂佸鏁婚崺娑㈠箣濠㈡繂缍婂畷妤呮嚃閳哄倸娅戦梻浣风串闂勫嫮鍒掗幘璇茶摕婵炴垯鍨洪崑鎰版煙妫颁胶鍔嶉柣锝夘棑缁辨挻鎷呴崫銉у姰婵＄偞娼欓幗婊堝箲閵忕姭鏀介悗锝庝簽閸婄偤鎮峰鍐闁轰礁绉撮…銊╁礃閿濆棙鏉搁梻浣虹帛閸旀牕顭囧▎鎾村€堕柛妤冨剱濞撳鎮楀☉娅虫垿宕愰幇鐗堢厱闁冲搫鍊诲ú瀵糕偓娈垮枟閹歌櫕鎱ㄩ埀顒勬煃闁款垰浜鹃梺褰掓敱濡炶棄顫忓ú顏勫窛濠电姴瀚悾鐢告煟鎼淬垹鍤柛銊ョ埣閺佹劙鎮欓悜妯绘珖闂佺鏈粙鎾诲储闁秵鐓欓柤鍦瑜把呯磼鏉堛劍绀嬬€殿喗鎮傚畷姗€顢欓悾灞藉妇闂備焦鎮堕崕顖炲礉鐏炵偓鍙忕€广儱妫庢禍婊堟煏韫囥儳纾块柟鍐叉川閳ь剝顫夊ú妯煎垝韫囨蛋鍥箮閼恒儳鍙嗗┑鐐村灦椤洨绮绘繝姘厵妞ゆ梻鍘уΣ缁樸亜椤撴粌濮傜€规洘锕㈤、鏃堝幢閺囩姷顦ㄧ紓鍌氬€搁崐椋庢閿熺姴闂い鏇楀亾鐎规洩缍佸畷姗€濡歌濞堥箖姊洪棃娴ュ牓寮插☉姘辨／鐟滄棃寮婚悢琛″亾濞戞瑯鐒界紒鐘虫崌閺岀喖骞栭悙娴嬪亾閺嶃劎鈹嶅┑鐘叉处閸婇攱銇勮箛鎾愁仱闁稿鎹囧浠嬵敇閻旇渹缃曟繝寰锋澘鈧洟宕锕€鍑犻柛顐熸噰閸嬫捇鐛崹顔煎濡炪倧瀵岄崹铏珶閺囥垹鍨傛い鏃囶潐閺傗偓闂備胶绮敋鐎殿喛鍩栧鍕礋椤栨稓鍘介梺瑙勫劤椤曨厼煤閹绢喗鐓涢悘鐐插⒔濞插瓨銇勯姀鈩冪闁轰焦鍔欏畷鍫曞煛婵犲倹鍊繝鐢靛Х椤ｄ粙宕滃┑濞夸汗闁告劦鍠栫粻鐔兼煥閻斿搫孝濡楀懘姊洪崨濠冨闁搞劍澹嗙划濠氬箮閼恒儳鍘搁梺绋挎湰缁嬫垿顢氬鍫熺厽闊洦鎸炬晶锔芥叏婵犲懏顏犻柟椋庡█閸ㄩ箖鎼归銈勭敖缂傚倸鍊风欢锟犲窗濡ゅ懏鍋￠柍鍝勬噽瀹撲線鏌涢幇闈涙灈閸ュ瓨绻濋姀锝嗙【闁挎洩绠撻弫宥咁煥閸愶絾鏂€闂佺粯顭堥婊冾啅閵夆晜鐓欑€瑰嫰鍋婇崕蹇涘础闁秵鐓熼柟杈剧到琚氶梺鎶芥敱鐢帡婀侀梺鎸庣箓濞层倝宕濈€ｎ喗鐓曢柕鍫濆€告禍楣冩⒒閸屾瑧顦︽い鎾茬矙瀵爼宕归鍛秵闂傚倷娴囬鏍疮椤愶箑鐐婇柕濞垮劗閸嬫捇鎮滈懞銉у幈濠电娀娼уΛ妤咁敂椤愶附鐓曢柡鍐╂尵閻ｈ鲸銇勯鍕殻濠碘€崇埣瀹曞崬螖閳ь剙顭囬幋锔解拺缂佸顑欓崕鎰版煙閻熺増鍠樼€殿喖顭烽幃銏ゅ礂閸忕厧鍔掓俊鐐€栭崝鎴﹀垂瑜版帪缍栫€广儱鎷嬪〒濠氭煏閸繂鏆欏┑鈩冩倐閺屾稒鎯旈妸锔介敪闂佷紮绲块崗妯虹暦婵傜鍗抽柕濠忛檮濞呭苯鈹戦悙鑸靛涧缂佽弓绮欓獮澶愭晸閻樺啿浠煎銈嗙墱閸嬬偤鍩涢幒鎳ㄥ綊鏁愰崨顔兼殘闂佽鍨伴悧鎾诲蓟濞戞瑧绡€闁告劏鏅涢埀顒佸姍閺岀喖顢欓惌顐邯閸╃偤骞嬮悩顐壕闁挎繂楠告禍婊堟煃瑜滈崜娆撴偉婵傜钃熺€广儱顦导鐘绘煕閺囥劌澧繛鍛€濆娲箮閼恒儲鏆犻柣銏╁灲缁绘繂顕ｇ拠娴嬫闁靛繒濮村畵鍡椻攽鎺抽崐鎾绘倿閿曞倹鍋熼柡鍐ㄧ墛閳锋垿姊洪銈呬粶闁兼椿鍨遍弲鍫曨敍濞戞氨顔曢梺鑲┾拡閸撴瑩寮稿☉銏＄厪闁糕剝娲滈ˇ锕傛懚閺嶎厽鐓曟繛鎴濆船閺嬫稑霉濠婂嫮鐭掓慨濠呮缁瑧鎹勯妸褜鍞洪梻浣告啞椤棝宕熼褎绁繝鐢靛仜濡﹥绂嶅┑瀣厱闁瑰濮风壕钘壝归敐鍫濅簵闁硅揪闄勯崑鍌炴煏婢跺棙娅嗛柣鎾存礋閺屾洝绠涢妷褍鍩岄梺钘夊閵堟悂寮婚弴銏犵倞闁靛鍎遍～鎺楁⒑鐠団€虫灀闁哄懐濞€楠炲啯绂掔€ｎ偄浠虹紒鐐緲椤﹁鲸鎯?
        // 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸ゅ嫰鏌涢锝嗙缁炬儳娼￠弻锝夊閵忊晝鍔搁梺姹囧€濈粻鏍蓟瀹ュ浼犻柛鏇ㄥ墮濞咃絿绱撴担鐟板妞ゃ劌锕濠氬灳閹颁礁鎮戦梺鍛婂姂閸斿矂鈥栨径濞炬斀闁绘劕寮堕崳钘夆攽閻愨晛浜鹃梻浣告惈閻寰婇崐鐔轰航闂備胶顭堢换鎰板触鐎ｎ€帡宕熼鍌滎啎闂佺懓顕崑鐐典焊椤撶姷纾煎璺猴功缁夎櫣鈧鍠涢褔顢樻總绋块唶妞ゆ劧缍嗛埀顒€娲缁樻媴閸涘﹤鏆堥梺瑙勬倐缁犳牕鐣烽悧鍫熷劅闁靛绠戞禒濂告⒑缂佹ê鐏卞┑顔哄€濆鏌ュ箹娴ｅ湱鍘藉┑鈽嗗灥椤曆呭緤閼姐倐鍋撶憴鍕碍婵☆偅绻傞～蹇涙惞閸︻厾锛滃┑鈽嗗灠閹碱偊锝炲畝鍕拺閻犲洩灏欑粻鏌ユ煠瑜版帞鐣洪柍銉畵瀹曠螖閳ь剟宕橀埀顒€顪冮妶鍡樺暗闁哥喎娼￠獮蹇涙惞閸︻厾锛濋梺绋挎湰閻熝囧礉瀹ュ棎浜滄い鎾跺仦閸犳﹢鏌熼姘辩劯妤犵偞甯￠獮姗€宕ｅΟ鑲╂晨闂傚倷鐒︾€笛呮崲閸屾粍宕查柟鎷屽焽閳ь剙鎳橀、妤呭礋椤掑倸骞嶉柣搴ｆ嚀鐎氼厽绔熼崱娆戠煋婵炲樊浜濋悡娆愩亜閺傛寧鎯堥柣蹇氬皺閳ь剝顫夊ú姗€銆冩繝鍥х畺闁斥晛鍟崕鐔兼煃閵夛箑澧俊顐㈡濮婂宕掑▎鎰偘濡炪倖娉﹂崨顏勪壕婵炴垶甯楀▍鍥╃磼椤旂晫鎳囨鐐村笒铻栧ù锝堫潐閻濄劌鈹戦悩鍨毄濠殿喕鍗冲畷褰掓偂鎼存ɑ鐏冨┑鐐村灦濮樸劑鎯岄幘鑸靛枑闁绘鐗嗙粭鎺楁煢閸愵亜鏋戠紒缁樼洴楠炲鈻庤箛鏇氭偅缂傚倷鑳舵慨鍨箾婵犲洤钃熼柨鐔哄Т绾惧吋鎱ㄥΟ鍧楀摵闁汇劍鍨垮娲传閸曢潧鍓伴梺鐟板暱闁帮綁宕洪悙鍝勭闁挎棁妫勯埀顒傚厴閺屾稑鈻庤箛锝喰﹀┑鐐叉嫅缂嶄礁顫忕紒妯诲缂佸娉曢惄搴ｇ磽閸屾氨小缂佽埖宀搁幃浼搭敋閳ь剙鐣峰鈧、娆撴寠婢跺绱﹂梻鍌欑閹诧繝骞愮粙娆惧殨闁割偅娲栭悿楣冩煥濠靛棭妲归柍閿嬪灩缁辨挻鎷呴惂闀愮返闂佺粯甯掓晶浠嬪焵椤掑喚娼愭繛鑼枎椤洩顦归柟?
        return this.removeById(id);
    }

    private String generateUniqueInviteCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        String code;
        while (true) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            code = sb.toString();

            // 濠电姷鏁告慨鐑藉极閸涘﹥鍙忛柣鎴濐潟閳ь剙鍊块幐濠冪珶閳哄绉€规洏鍔戝鍫曞箣閻欏懐骞㈤梻鍌欑窔閳ь剛鍋涢懟顖涙櫠閹绢喗鐓涢悘鐐插⒔閵嗘帡鏌嶈閸撱劎绱為崱娑樼；闁告侗鍘鹃弳锔锯偓鍏夊亾闁告洦鍓涢崢鎼佹倵閸忓浜鹃柣搴秵閸撴盯鏁嶉悢鍝ョ閻庣數顭堥鎾斥攽閳ヨ櫕鍠樻鐐茬箻閹晝鎷犻懠顒夊斀闂備礁婀遍崕銈夊春閸繍鐒介柕澶嗘櫆閳锋帡鏌涚仦鍓ф噮妞わ讣绠撻弻鐔哄枈閸楃偘绨婚柧鑽ゅ仦缁绘盯骞嬪▎蹇曚患缂備焦鍔栭〃濠囧蓟閳ユ剚鍚嬮幖绮光偓宕囶啋闁诲氦顫夐幐椋庢濮樿泛钃熼柣鏂挎啞缂嶅洭鏌熼鍡楁噺琚欓梻鍌欐祰濡椼劎绮堟笟鈧弫鍐敂閸繆鎽曢梺闈涚墕閹潡宕烽娑樹壕闁挎繂楠告禍婵嬫煟濠靛洦绀嬫慨濠傛惈鏁堥柛銉戝喚鐎撮梻浣规偠閸斿本鏅舵惔銊ョ闁靛繒濮Σ鍫ユ煏韫囨洖啸妞ゆ挻妞藉铏圭磼濡搫顫岄梺瀹︽澘濮傜€规洘娲橀幆鏃堝Ω閿旇瀚奸梻浣告啞缁嬫垿鈥﹂鍕€块柣鎰靛厸缁诲棙銇勯幇鈺佺仼妞ゅ繆鏅濋埀顒冾潐閹稿摜鈧碍婢橀～蹇斻偊鐟併倓姹楅梺鍦劋閹碱偆妲愰悽鐢电＝濞达絾褰冩禍鐐箾鏉堝墽鎮奸柛搴涘€濆畷浼村幢濞戞瑧鍙嗛梺鍝勬川閸嬫盯鍩€椤掆偓缂嶅﹪骞冮敓鐘参ㄩ柍鍝勫€婚崢鎼佹⒑閹肩偛鍔撮柣鎾崇墕閳绘捇寮Λ鐢垫嚀椤劑宕奸姀銏℃瘒婵犳鍠栭敃銉ヮ渻閽樺鏆﹂柕濠忓缁♀偓闂佺鏈划宀€鏁惔锝囩＝闁稿本鐟︾粊鏉款渻鐎靛壊鐒鹃悡銈夋煟閺冨倸甯堕柡鍕╁劦閺屽秷顧侀柛鎾村哺婵＄敻宕熼姘鳖唺閻庡箍鍎遍悧鍡涘储閿熺姵鈷戦柟棰佺閻忊剝绻涢懠顒€鏋涚€殿喖顭锋俊鎼佸Ψ椤旇棄鍏婃俊鐐€栭幐鐐叏閻戣姤鍎婇柛顐犲劜閳锋垿姊洪銈呬粶闁兼椿鍨遍弲鍓佲偓娑櫱滄禍婊堟煏韫囧﹤澧叉い銉ョ墢閳ь剝顫夊ú鏍х暦椤掑啰浜介梻浣瑰劤缁绘帡骞婇幘缁樼厐闂侇剙绉甸埛鎺楁煕鐏炲墽鎳呯紒鎰⒐缁绘稒鎷呴崘鍙夌〗闁搞儺鍓﹂弫鍐煥閺囨浜鹃梺姹囧€楅崑鎾舵崲濞戙垹绠ｆ繛鍡楃箳娴犲ジ姊虹紒妯虹瑨闁诲繑宀告俊鐢稿礋椤栨氨顔婇梺绯曞墲閵囩偞绔熼弴鐔剁箚闁绘劦浜滈埀顒佺墵瀹曞綊妫冨ù銏℃そ椤㈡﹢濮€閵忕姷銈﹂梻濠庡亜濞诧妇绮欓幒妤€纾婚柟鎵閻撴瑥螞妫颁浇鍏岄柛鏂跨Ч瀵偊宕奸妷锔规嫼缂佺虎鍘奸幊搴ㄋ夊澶嬬厵婵炶尪顔婄花鐣屸偓鍨緲閿曘儳绮嬮幒鏂哄亾閿濆骸浜滈柍?
            Long count = baseMapper.selectCount(new QueryWrapper<Clazz>().eq("invite_code", code));
            if (count == 0) break; // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｉ幇顒佹儓闁搞劌鍊块弻娑㈩敃閿濆棛顦ョ紓浣哄С閸楁娊寮诲☉妯锋斀闁告洦鍋勬慨銏ゆ⒑濞茶骞楅柟鐟版喘瀵鎮㈤搹鍦紲闂侀潧绻掓慨鐢告倶瀹ュ鈷戠紒瀣儥閸庡秹鏌涙繝鍐疄鐎殿喖顭峰鎾偄閾忚鍟庨梻浣虹帛閸旓箓宕滃鑸靛仧闁哄啫鐗婇埛鎴︽煕閿旇骞楅柛銈傚亾闂備胶绮悧鏇㈠Χ缁嬫鍤曞┑鐘宠壘閸楁娊鏌ｉ弮鍥仩妞ゆ梹娲熷娲礈閼碱剙甯ラ梺鍝ュУ閻楁骞堥妸锔藉劅闁靛鑵归幏娲煟閻樺弶绌块悘蹇旂懅缁骞庨懞銉у幈闂佺粯锚绾绢厽鏅堕崹顐闁绘劖褰冮弳銏ゆ煏閸ャ劌濮嶆鐐村浮楠炲顭ㄩ崨顖氣偓鐐烘⒒閸屾瑧顦﹂柟娴嬧偓瓒佹椽鏁冮崒姘憋紱闂佸憡渚楅崢婊堝箳濡も偓缁犳盯鏌ｅΔ鈧悧鍐箯濞差亝鈷戠痪顓炴噹娴滃綊鏌涚€ｎ偆娲撮柛鈹惧亾濡炪倖鍨堕崕鎶藉焵椤掍胶绠撻柣锝囧厴婵偓闁挎稑瀚板顕€姊洪崨濠冨碍鐎殿喖鐖煎鏌ヮ敂閸啿鎷洪梻鍌氱墛娓氭危閹绢喗鐓涢柛娑变簼濞呭﹦鈧鍣崑濠傜暦閹烘鍊烽悗鐢殿焾楠炲牓姊绘担瑙勭伇闁哄懏鐩畷鏉款潩椤戣姤鐏佸┑顔姐仜閸嬫捇鏌″畝瀣М闁轰焦鍔栧鍕暆閳ь剟寮抽鈶╂斀妞ゆ梻銆嬮弨缁樹繆閻愭壆鐭欓柣娑卞櫍楠炲洭顢橀悢宄板Τ闂備焦瀵х换鍌炲磹瑜版帪缍栫€广儱顦伴埛鎺懨归敐鍥剁劸闁诡喗鍨舵穱濠囨倷閸欏浠撮悗瑙勬礀缂嶅﹪骞冮悾宀€鐭欓悹鍥ㄦ嫕缁犳捇寮婚悢鍏煎亱闁割偆鍠撻崙锟犳⒑濞茶骞楁い銊ユ嚇閳ワ妇鎹勯妸锕€纾梺鎯х箰濠€閬嶅礉缁嬪簱鏀介柣鎰摠缂嶆垿鎮楀顓熺凡妞ゎ偄绻愮叅妞ゅ繐鎷嬪Λ鍐ㄢ攽閻愭潙鐏卞瀛樼摃閸婃挳姊婚崒娆掑厡妞ゎ厼鐗撻弫鍐Ψ閵夊啯妞介幃銏ゆ偂鎼淬垺袣闂備線娼ф蹇曞緤閸ф鍋柍褜鍓欓埞鎴︽倷閺夋垹浠搁梺鑽ゅ暀閸パ冨亶?
        }
        return code;
    }

    @Override
    public boolean inviteStudentToClass(Integer classId, Long studentId) {
        // 1. 濠电姷鏁告慨鐑藉极閸涘﹥鍙忛柣鎴濐潟閳ь剙鍊块幐濠冪珶閳哄绉€规洏鍔戝鍫曞箣閻欏懐骞㈤梻鍌欑窔閳ь剛鍋涢懟顖涙櫠閹绢喗鐓涢悘鐐插⒔閵嗘帡鏌嶈閸撱劎绱為崱娑樼；闁告侗鍘鹃弳锔锯偓鍏夊亾闁告洦鍓涢崢鎼佹倵閸忓浜鹃柣搴秵閸撴盯鏁嶉悢鍝ョ閻庣數顭堥鎾斥攽閳ヨ櫕鍠樻鐐茬箻閹晝鎷犻懠顒夊斀闂備礁婀遍崕銈夊春閸繍鐒介柕澶堝劗閺€浠嬫煟濡椿鍟忛柡鍡╁灦閺屽秷顧侀柛鎾寸懇瀹曨垳鎹勯妸褎锛忛悷婊勬瀵鎮㈢喊杈ㄦ櫓闂佺粯鎸哥花鍫曞磻閹捐绠瑰ù锝囨嚀娴犲ジ姊虹紒妯虹伇婵☆偄瀚弫顕€姊绘担绋挎毐闁圭⒈鍋婂畷鎰版偡閹佃櫕鐎洪梺鎼炲労閸撴岸鍩涢幋锔解拺妞ゆ劑鍊曟禒婊堟煠濞茶鐏￠柡鍛閳ь剛鏁哥涵鍫曞磻閹捐埖鍠嗛柛鏇ㄥ墰椤︺劑姊洪懡銈呮毐闁哄懐濮撮锝夘敃閵忊晛鎮戞繝銏ｆ硾閿曪箓藝閺夋娓婚柕鍫濇鐏忛潧鈹戦姘煎殶婵″弶鍔欏濠氬Ψ閿旇瀚藉┑鐘垫暩婵瓨顨ラ崨濠勵洸婵犲﹤瀚粻楣冩偣閸ュ洤鎳愰弳銈呂旈悩闈涗沪闁告梹鐗滈幑銏犫攽閸♀晜鍍甸梺鍛婎殘閸嬫稓寰婇悙顑跨箚闁绘劦浜滈埀顒佺墱閺侇噣骞掗弬鍝勪壕婵鍘у顕€鏌熼鍡欑瘈闁诡喓鍨藉畷顐﹀Ψ瑜忓Σ鍥⒒娴ｇ顥忛柣鎾崇墦瀹曟垿宕熼浣稿伎闂佹悶鍎洪悡鍫濃枔娴犲鐓熼柟閭﹀枟閻撳繑銇勮箛鎾村窛濞存粍鐟╁缁樻媴閾忕懓绗￠梺鎸庢皑閳ь剚顔栭崰娑㈠春閺嵮屽殫闁告洦鍨扮粈瀣亜閹捐泛啸闁?
        ClassStudent exist = classStudentMapper.selectOne(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, classId)
                .eq(ClassStudent::getStudentId, studentId));
        
        if (exist != null) {
            if (exist.getStatus() == 0) {
                // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柛娑橈攻閸欏繘鏌ｉ幋婵愭綗闁逞屽墮閸婂湱绮嬮幒鏂哄亾閿濆簼绨介柛鏃撶畱椤啴濡堕崱妤€娼戦梺绋款儐閹瑰洭寮诲☉銏″亜闂佸灝顑呮禒鎾⒑缁洘鏉归柛瀣尭椤啴濡堕崱妤€娼戦梺绋款儐閹稿墽妲愰幘鎰佸悑闁告粌鍟抽崥顐⑽旈悩闈涗粶闁哥噥鍋夐悘鎺楁煟閻樺弶绌块悘蹇旂懅缁綁鎮欓悜妯锋嫼閻熸粎澧楃敮鎺撶娴煎瓨鐓曢柟鎯ь嚟閹冲啯銇勯弴顏嗗埌闁伙綇绻濋弻鍥晜閽樺妲ｉ梻鍌欑窔濞佳囨偋閸℃あ娑樷槈濞嗘劖鐝峰銈嗗笒閸婅崵澹曢崗绗轰簻闁哄啫娲ゆ禍褰掑极閸儲鍊垫繛鍫濈仢閺嬬喎鈹戦悙璇ф敾濞ｅ洤锕幊婊堟濞戞氨鐛梺璇插嚱缂嶅棙绂嶅▎鎾崇闁哄稁鍘介埛鎺懨归敐鍫綈闁稿濞€閺屾稒鎯旈敐鍛缂備礁鍊哥粔鎾煘閹寸姭鍋撻敐搴′簻婵炲牊锕㈤幃宄邦煥閸涱収鏆柣銏╁灡椤ㄥ﹪骞冮敓鐘冲亹缂備焦顭囬崢鍗烆渻閵堝骸骞楅柛銊ョ仛缁傚秴顭ㄩ崼鐔哄幗闂婎偄娲﹀鍦焊閿曞倹鐓涢悘鐐殿焾婢ф煡鏌熷畡鐗堝殗鐎规洦鍋婃俊鐑藉箛閳轰胶浼堥梺鍝勭焿缂嶄線寮幇鏉垮耿婵炲棙鍨归弶浠嬫⒒娴ｈ櫣銆婇柡鍌欑窔瀹曟垿骞橀幇浣瑰瘜闂侀潧鐗嗗Λ妤呭锤婵犲洦鐓曢悗锝庡亝瀹曞矂鏌熼鍡欑瘈濠德ゅ煐瀵板嫮鈧綆鍓欓獮鍫ユ煟閻斿摜鐭婇梺甯秮婵℃挳宕橀鐓庣獩濡炪倖鐗楃划鍫㈢箔婢舵劖鈷戦悷娆忓閸旇泛鈹戦鍝勨偓婵嬪箖闄囩粻娑㈠箼閸愵亜鏁搁梻浣哄帶閹芥粓寮粙妫佃櫣鈧稒眉缁诲棛绱撴担闈涚仼婵炲懎绉归弻鈥崇暆鐎ｎ剛鐦堥悗瑙勬礋娴滃爼宕洪埄鍐╁闁告縿鍎洪崯鍥⒒閸屾瑧顦﹂柣銈呮搐铻為柛鏇ㄥ瀬閸ヮ剚鍋ㄩ柛婵勫劚缁侊箑鈹戦濮愪粶闁稿鎹囬弻鐔兼偂鎼达絿楔濡炪們鍨哄ú鐔煎极閹版澘宸濇い鎾跺剱濡劙姊婚崒姘偓鎼佸磹妞嬪海鐭嗗〒姘ｅ亾妤犵偛顦甸弫鎾绘偐閸欏袣濠电姷鏁告慨瀵糕偓姘煎幖閻ｇ兘宕ｆ径宀€鐦堥梻鍌氱墛缁嬫帡藟閻樼粯鐓涢柛鈽嗗弮濡绢喚绱掔紒妯肩疄婵☆偄鍟埥澶愬础閻愭彃顥庨梻鍌欒兌椤牓顢栭崱娑樼婵炲棙鎸搁拑鐔兼煟閺冨洦顏犵痪鎯у悑娣囧﹪顢涘┑鍥朵哗闂佽崵鍠嗛崝宀勫煘?
                exist.setStatus(1);
                return classStudentMapper.updateById(exist) > 0;
            }
            return true; // 闂傚倸鍊搁崐宄懊归崶顒夋晪鐟滃秹锝炲┑瀣櫇闁稿矉濡囩粙蹇旂節閵忥絽鐓愰柛鏃€鐗犲畷鎴﹀Χ婢跺鍘搁梺鎼炲劗閺呮稑鐨梻浣虹帛鐢帡鏁冮妷褎宕叉繛鎴欏灩楠炪垺淇婇婵囶仩濞寸姵鎸冲娲箹閻愭彃顬夌紓浣筋嚙閻楁挸顕ｆ繝姘╅柍鍝勫€告禍婊堟⒑閸涘﹦绠撻悗姘嚇婵偓闁靛牆妫涢崢鍗炩攽閻愬弶顥滃Δ鐘茬箻瀵劍绻濆顓犲帾闂佹悶鍎滈崘鍙ョ磾闁诲孩顔栭崰鏇犲垝濞嗘劒绻嗘慨婵嗙焾濡插嘲鈹戦埄鍐ㄧ祷闁绘鎸搁～蹇涙惞鐟欏嫬鐝伴梺鑲┾拡閸撴盯顢欓崶褉鏀介柣鎰级閳绘洟鏌涘▎蹇撴殭妞?
        }
        
        // 2. 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极瀹ュ绀嬫い鎺嶇劍椤斿洦绻濆閿嬫緲閳ь剚娲熷畷顖涘鐎涙ê鍓瑰┑鐐叉閹稿鎮″▎鎾村€甸柣銏☆問閻掗箖宕崫銉х＜闁诡垎鍐◤缂備緡鍠楅悷鈺侇嚕婵犳碍鍋勯柣鎾虫捣椤斿姊洪柅娑樺祮婵炰匠鍐ｆ灁妞ゆ挾鍋愰弨浠嬫煃閽樺顥滃ù婊勭箞閺屻劑寮村Ο铏逛紙閻庢鍠栭…鐑藉箖閵忊槅妲归幖娣灪閻ゅ倿姊绘担绛嬫綈妞ゆ梹鐗犲畷鏉款潩鐠鸿櫣顦梺褰掓？閻掞箓鍩涢幋锔界厱婵犻潧瀚崝姘归悩宕囩煀妞ゎ叀鍎婚ˇ鏌ユ⒑鐢喚绉柣?
        ClassStudent cs = new ClassStudent();
        cs.setClassId(classId);
        cs.setStudentId(studentId);
        cs.setJoinTime(LocalDateTime.now());
        cs.setStatus(1); // 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸ゅ嫰鏌涢锝嗙缁炬儳娼￠弻锝夊閵忊晝鍔搁梺姹囧€楅崑鎾舵崲濞戙垹绠ｆ繛鍡楃箳娴犻箖姊虹粙娆惧剭闁稿﹥绻堝濠氬即閻旇櫣顔曢悷婊冪Ф閳ь剚鍑归崢濂稿煝閹炬潙顕遍悗娑欘焽閸樹粙鏌熼崗鑲╂殬闁告柨绉撮埥澶庮樄闁哄瞼鍠栭幊鐐哄Ψ瑜忛悡鍌涚節閵忥綆娼愭繛鑼枛楠炲啫鈻庨幙鍐╂櫈闂佸憡鐟ｉ崕鎶芥儎鎼淬劍鈷掑ù锝呮啞閹牓鏌涢悢鍝勨枅鐎规洘鍨块獮妯肩礄閻樼數鐣鹃梻浣告啞閻熴儵藝椤栨粎绠旂憸蹇曟閹烘鏁婇柤娴嬫櫅閳亶姊洪柅鐐茶嫰婢у墽绱撳鍛棦鐎规洘顨呴～婊堝焵椤掆偓閻ｇ兘骞嬮敃鈧粻鑽ょ磽娴ｅ顏呯椤撱垺鈷戠紓浣癸供濞堟棃鏌ｅΔ鈧Λ婵嗩嚕閹惰棄閱囬柕澶涚畱娴狀厼鈹戦悩璇у伐閻庢凹鍘惧▎銏ゅ蓟閵夛妇鍘遍梺缁樻磻缁€渚€鎮橀敂绛嬫闁绘劘灏欐晶锕€鈹戦埄鍐╁€愰柛鈺嬬節瀹曟帒顫濋敐鍛闂佺粯鍨兼慨銈夋偂閺囩喓绡€闂傚牊绋掗ˉ婊勩亜韫囷絽浜濋柕?
        
        return classStudentMapper.insert(cs) > 0;
    }

    @Override
    public List<Map<String, Object>> getStudentClasses(Long studentId) {
        // 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋為悧鐘汇€侀弴銏犖ч柛鈩冦仦缁剝淇婇悙顏勨偓鏍礉瑜忕划濠氬箣閻樺樊妫滈梺绉嗗嫷娈曢柣鎾寸懅缁辨挻鎷呴棃娑氫患濠电偛鎳忛敃銏ゅ蓟閳╁啯濯撮柛鎾村絻閸撹鲸绻涢敐鍛悙闁挎洦浜濇穱濠囧醇閺囩偛绐涘銈嗘尵閸犳劙鎮挎笟鈧缁樻媴娓氼垳鍔稿銈嗗灥濡繈鐛幇鏉跨濞达絽鎽滈崣鈧┑鐘灱閸╂牠宕濋弴鐘电焼闁告劦浜栭崑鎾绘偡閺夋妫岄梺鍝ュУ椤ㄥ﹤鐣烽弶娆炬建闁逞屽墮椤繒绱掑Ο璇差€撻梺鍏间航閸庮垶鍩€椤掆偓閸熸挳寮婚敐澶婄闁绘劗鏁搁弳銈呂旈悩闈涗沪闁搞劌鐏濋悾鐑藉箚闁附鞋闂備礁鎼Λ鎾⒔閸曨厽宕叉繝闈涱儐椤ュ牊绻涢幋婵嗚埞闁告捇浜跺铏圭矙濞嗘儳鍓遍梺鍛婃⒐閻熲晛顕ｆ繝姘亜闁惧繐婀遍敍婊冣攽閳藉棗鐏ユ繛鍜冪秮閹兘濡搁妷銏℃杸濡炪倖姊归娆忣焽閹扮増鐓熼柟缁㈠灙閸嬫捇骞囨担鍦▉闂備礁鎲＄换鍌溾偓姘嚀椤﹀湱鈧娲橀崕濂杆囬崣澶堜簻妞ゆ巻鍋撻柣妤佹崌瀵鏁愰崱妯哄妳闂侀潧绻掓慨鏉懶掗崼銉︹拺闁告稑鈯曢懖鈺佸灊婵炲棗娴氶崵鏇㈡煠绾板崬澧€规洖顦妴鎺戭潩閻撳海浠╅梺璇″枟閸ㄥ墎鎹㈠┑瀣潊闁挎繂妫涢妴鎰版⒑閹稿孩纾搁柛濠冪箞楠炲啴妫冨ù宕囧枛閹筹繝濡堕崶鈺傤潓闂傚倷绶氬褎顨ヨ箛鏇炵筏闁告挆鍕幑婵°倧绲介崯顖炴偂閻樺磭绠鹃柤濂割杺閸ゆ瑩鏌ｈ箛銉х暤闁哄瞼鍠栭、娆撳礂閻撳簼娣柣搴ゎ潐濞叉牕鐣烽鍕厺閹兼番鍔岀粻锝夋煟濞嗗繑鍣介幖鏉戯躬濮婂宕掑▎鎺戝帯缂備緡鍣崹鍫曞箖閻戣姤鏅濋柛灞剧☉娴狀參姊洪棃娑崇础闁告侗鍨抽崢鐘绘⒒娴ｅ憡鍟為柟鍝ュ厴閹虫宕奸弴鐔峰壒闂佺硶鍓濈粙鎺楁偂閺囥垺鐓涢柛銉ｅ劚婵″ジ鏌ｈ箛銉х瘈闁哄矉缍侀獮妯尖偓娑欘焽椤︿即姊洪崫鍕槵闁逞屽墮绾绢參寮抽妶鍡愪簻闁哄啫鍊哥敮鑸点亜韫囧﹥娅嗙紒缁樼洴瀹曘劑顢曢埗鈺佷壕婵犻潧顑呴拑鐔兼煥濠靛棙顥為柛搴ｅ枛閺屾洝绠涢弴鐐愩垺鎱ㄩ敐鍛ⅵ婵﹥妞藉畷銊︾節韫囨埃鍋撶捄銊х＜闁绘ê纾晶鐢碘偓瑙勬礃濡炰粙宕洪埀顒併亜閹哄秹妾峰ù婊勭矒閺岀喖宕崟顒夋婵炲瓨绮撶粻鏍ь潖濞差亜宸濆┑鐐寸閸ㄥ綊宕氶幒妤€绠荤€规洖娲﹀▓鎯р攽閻樿宸ラ柛鐔哄█瀵劍绂掔€ｎ偄鈧敻鏌ㄥ┑鍡涱€楀ù婊勭矊闇夋繝濠傜墔閹茬偓鎱ㄦ繝鍐┿仢妤犵偛妫滈ˇ宕囩磼閸屾凹鍎旈柡灞剧〒閳ь剨缍嗘禍婵嬎夐姀銈嗙厱闁崇懓鐏濋悘鑼偓娈垮櫘閸ｏ綁宕洪埀顒併亜閹烘垵顏╁鍛存⒑閸涘﹥澶勯柛銊﹀缁粯绻濆顓炰化婵°倧闄勭€笛囧Υ閸愨晝绠剧€光偓婵犱線鍋楅梺鍝勭焿缁查箖骞嗛弮鍫稏妞ゆ挻绻傞銏ゆ⒑鐠囪尙绠扮紒缁樺灴閹兘鍩￠崨顓熸К闂侀€炲苯澧柕鍥у楠炴帡骞嬪┑鍐ㄤ壕鐟滅増甯掗崥褰掓煃瑜滈崜鐔奉潖閾忚鍏滈柛婊€绀佸▓婵堢磽娴ｈ櫣甯涚紒璇茬墦閻涱噣宕橀鑺ユ闂佺粯顭堝▍鏇炩枍閵忋倖鈷戠紓浣癸供閻掗箖鎮樿箛鏃傛噰鐎规洘绻堥弫鍐焵椤掑嫧鈧棃宕橀鍢壯囨煕閳╁喚娈橀柣鐔稿姇閳规垿鏁嶉崟顐℃勃闁汇埄鍨遍〃鍫㈠垝鐎ｎ亶鍚嬪璺侯煬濞煎﹪姊洪棃娑氱疄闁搞劌顭烽弫宥咁煥閸喓鍘介梺缁樏鑸靛緞閸曨厽鍠愰柡澶婄仢閺嗭絿鈧鍠楁繛濠囧极閹邦厼绶為悗锝庡墮楠炲秵淇婇悙顏勨偓鏍偋濡ゅ懏鍤屽Δ锝呭暙濮规煡鏌ㄥ┑鍡樼闁稿鎸鹃幉鎾礋椤掑偆妲堕梻浣告憸婵敻鎮ч弴銏″仼鐎瑰嫰鍋婇悡銉╂煕閺囥劌澧い搴㈡尰缁绘盯骞橀弶鎴濇瘓闂佹悶鍔屽畷顒勬偤?
        List<ClassStudent> relations = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>().eq(ClassStudent::getStudentId, studentId)
        );

        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> classIds = relations.stream().map(ClassStudent::getClassId).collect(Collectors.toList());
        List<Clazz> classes = this.listByIds(classIds);
        Map<Integer, Clazz> classMap = classes.stream().collect(Collectors.toMap(Clazz::getId, c -> c));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ClassStudent relation : relations) {
            Clazz clazz = classMap.get(relation.getClassId());
            if (clazz != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", clazz.getId());
                map.put("className", clazz.getClassName());
                map.put("courseName", clazz.getCourseName());
                map.put("teacherId", clazz.getTeacherId());
                // 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕閻庤娲忛崕鎶藉焵椤掑﹦绉甸柛鐘崇墱婢规洟宕稿Δ浣哄幍闂佽鍨虫晶妤吽夋径鎰闁哄鍩婇煬顒勬煛鐏炶鈧繈骞婂┑瀣妞ゆ棁鍋愭晶顖氣攽閻愯尙鎽犵紒顔肩灱缁辩偞绻濋崶褑鎽曞┑鐐村灟閸ㄧ懓鏁俊鐐€栧濠氬储瑜旈敐鐐侯敂閸啿鎷洪梺纭呭亹閸嬫盯鍩€椤掍礁濮嶇€规洘鍨块獮姗€寮妷锔芥澑闂備胶绮崝姗€宕洪弽顑句汗鐟滃繒妲愰幒妤冨彄妞ゆ挾濮烽悡鎾澄旈悩闈涗沪妞ゃ劌鎳橀垾锕傚Ω閳轰線鍞堕梺缁樻濞撳綊鎮芥繝姘拺闁荤喐婢橀幃鎴︽煟閿濆簼閭€规洘绻傞鍏煎緞鐎ｎ亙绨甸梻浣告惈濞层劑宕伴幇鏉跨煑闊洦姊归崣蹇斾繆閵堝倸浜惧┑鈽嗗亝椤ㄥ棝寮查崼鏇炵妞ゆ梻鎳撴禍鐐箾閹寸偟鎳呯紒鐘靛仦閹?
                User teacher = userMapper.selectById(clazz.getTeacherId());
                map.put("teacherName", teacher != null ? (teacher.getNickname() != null ? teacher.getNickname() : teacher.getUsername()) : "Unknown");
                map.put("joinTime", relation.getJoinTime());
                map.put("status", relation.getStatus()); // 0:闂傚倸鍊搁崐鎼佸磹瀹勬噴褰掑炊椤掑鏅悷婊冮叄閵嗗啴濡烽埡浣侯啇婵炶揪绲块…鍫ュ磻瀹ュ應鏀介柣妯款嚋瀹搞儵鏌ｅΔ鈧Λ娑氬垝閸儱閱囬柣鏃囨閻﹀牓姊婚崒姘卞濞撴碍顨婂畷鏇㈠箛閻楀牏鍘遍柣搴秵娴滃爼宕曢弮鍫熺厸閻忕偟鍠庡Λ娑㈡偪閳ь剟姊洪幐搴ｇ畵闁瑰啿绉堕懞杈ㄧ鐎ｎ偀鎷? 1:闂傚倸鍊搁崐宄懊归崶顒夋晪鐟滃秹婀侀梺缁樺灱濡嫮绮婚悩缁樼厵闁硅鍔﹂崵娆戠磼閻橀潧鏋涙慨濠佺矙瀹曞爼顢楅埀顒侇攰闂備礁婀辨晶妤€顭垮Ο鑲╃焼闁告劦鍠楅悡鍐煕濠靛棗顏╅柍褜鍓涢…鍫ヮ敋閿濆閿ゆ俊銈勮閹疯櫣绱撻崒娆戝妽闁挎艾鈹戦鍏煎枠闁哄瞼鍠撻崰濠囧础閻愭壆鏁栫紓?
                result.add(map);
            }
        }
        return result;
    }

    @Override
    public String joinClass(Long studentId, String inviteCode) {
        // 1. 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋為悧鐘汇€侀弴銏犖ч柛銉㈡櫇閸橆垶姊绘担鍛婂暈婵炶绠撳畷銏ゆ寠婢跺本娈鹃梺鍛婄懃椤﹁京寮ч埀顒勬⒒閸屾氨澧涘〒姘殜瀹曟洟骞囬悧鍫㈠幈闁诲函缍嗛崐鏍箣閻樺啿搴婂┑鐐村灟閸ㄥ綊鐛姀鈥茬箚妞ゆ牗绻嶉崵娆撴⒒婢跺﹦效婵﹨娅ｇ槐鎺懳熼搹鍦啰缂傚倷绶￠崰妤呮偡閳轰緡鍤曢悹鍥ф▕閸氬鏌涢顐簼妞ゅ骸娲缁樻媴閸涘﹤鏆堥梺鍦焾椤兘骞嗛崟顖ｆ晬闁绘劙娼х粊锕傛椤愩垺澶勭紒瀣浮瀹曟垿骞囬悧鍫㈠幍婵☆偊顣﹂懗鍫曞礉瀹ュ鏋侀柛顐犲劜閳锋垹鐥鐐村櫣妞ゃ儱绉归弻鐔访虹拋宕囨晼閻庡灚婢橀敃銉х矉閹烘柡鍋撻敐搴′簻闁?
        Clazz clazz = this.getOne(new LambdaQueryWrapper<Clazz>().eq(Clazz::getInviteCode, inviteCode));
        if (clazz == null) {
            return "Invalid invite code";
        }

        // 2. 濠电姷鏁告慨鐑藉极閸涘﹥鍙忛柣鎴濐潟閳ь剙鍊块幐濠冪珶閳哄绉€规洏鍔戝鍫曞箣閻欏懐骞㈤梻鍌欑窔閳ь剛鍋涢懟顖涙櫠閹绢喗鐓涢悘鐐插⒔閵嗘帡鏌嶈閸撱劎绱為崱娑樼；闁告侗鍘鹃弳锔锯偓鍏夊亾闁告洦鍓涢崢鎼佹倵閸忓浜鹃柣搴秵閸撴盯鏁嶉悢鍝ョ閻庣數顭堥鎾斥攽閳ヨ櫕鍠樻鐐茬箻閹晝鎷犻懠顒夊斀闂備礁婀遍崕銈夊春閸繍鐒介柕澶堝劗閺€浠嬫煟濡椿鍟忛柡鍡╁灦閺屽秷顧侀柛鎾寸懇瀹曨垳鎹勯妸褎锛忛悷婊勬瀵鎮㈢喊杈ㄦ櫓闂佺粯鎸哥花鍫曞磻閹捐绠瑰ù锝囨嚀娴犲ジ姊虹紒妯虹伇婵☆偄瀚弫顕€姊绘担绋挎毐闁圭⒈鍋婂畷鎰版偡閹佃櫕鐎洪梺鎼炲労閸撴岸鍩涢幋锔解拺妞ゆ劑鍊曟禒婊堟煠濞茶鐏￠柡鍛閳ь剛鏁哥涵鍫曞磻閹捐埖鍠嗛柛鏇ㄥ墰椤︺劑姊洪懡銈呮毐闁哄懐濮撮锝夘敃閵忊晛鎮戞繝銏ｆ硾閿曪箓藝閺夋娓婚柕鍫濇鐏忛潧鈹戦姘煎殶婵″弶鍔欏濠氬Ψ閿旇瀚藉┑鐘垫暩婵瓨顨ラ崨濠勵洸婵犲﹤瀚粻楣冩偣閸ュ洤鎳愰弳銈呂旈悩闈涗沪闁告梹鐟╅悰顕€寮介妸褏鎳濇繝鐢靛Т鐎氼噣鍩ｉ妶澶嬧拻濞达絿鎳撻婊呯磼鐠囨彃鈧儻妫熸繛瀵稿帶閻°劑宕戦崒鐐寸厸闁搞儯鍎遍悘顕€鏌涘▎鎰磳闁哄本鐩俊鐑藉箣濠靛﹤顏梺鍛婃尫閸楀啿顫忓ú顏勫窛濠电姴鍟犻幏鍦磽娓氬洤鏋熼柟鐟版搐椤曪絿鎷犲ù瀣潔闂侀潧绻掓慨鍫ュ煛閸涱喚鍙嗗┑鐘绘涧濡瑩宕崇憴鍕╀簻妞ゆ劑鍨洪崰妯绘叏婵犲懏顏犻柟椋庡█閸ㄩ箖鎼归銈勫闂傚倷绀侀幖顐﹀箯鐎ｎ喖纾诲┑鐘叉搐閺勩儵鏌嶈閸撴岸濡甸崟顖氱闁糕剝銇炴竟鏇熺節閻㈤潧袥闁稿鎹囧鍫曞醇濞戞ê顬堥梺鎼炲妽濮婂綊濡甸崟顖氱闁告鍋熸禒濂告⒑閸涘﹨澹樼紓宥咃躬瀵鏁愭径濠勭杸濡炪倖甯婇悞锕傚磿閹惧墎纾?
        Long count = classStudentMapper.selectCount(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getClassId, clazz.getId())
                        .eq(ClassStudent::getStudentId, studentId)
        );
        
        if (count > 0) {
            return "Already joined or pending review";
        }

        // 3. 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极瀹ュ绀嬫い鎺嶇劍椤斿洦绻濆閿嬫緲閳ь剚娲熷畷顖涘鐎涙ê鍓瑰┑鐐叉閹稿鎮″▎鎾村€甸柣銏☆問閻掗箖宕崫銉х＜闁诡垎鍐◤缂備緡鍠楅悷鈺侇嚕婵犳碍鍋勯柣鎾虫捣椤︻參鎮峰鍐闁轰緡鍠栭埥澶愬閿涘嫬骞堥梺璇插嚱缁插宕濈仦鐐弿闁稿瞼鍋為悡娆愩亜閺冨倻鎽傛繛鍫熺矌閳ь剝顫夊ú婊堝礂濮椻偓閵嗕礁顫滈埀顒勫箖濠婂牆骞㈤煫鍥ㄦ尫婢规洟鎮楅崗澶婁壕闂佸憡娲﹂崜娑㈠储闁秵鈷戦梻鍫熺〒婢ф洘绻涚仦鍌氣偓鏍矉閹烘鐒肩€广儱妫涢崢闈涱渻閵堝棙鈷愭い鎴炵懃椤洭骞囬悧鍫㈠幍閻庣懓瀚晶妤呭闯娴犲鐓欓柛娆忣槹鐏忥妇鈧娲滈崰鏍€佸鈧幃娆撳矗婢舵ê鎮戠紓?
        ClassStudent relation = new ClassStudent();
        relation.setClassId(clazz.getId());
        relation.setStudentId(studentId);
        relation.setJoinTime(LocalDateTime.now());
        relation.setStatus(0); // 0: 闂傚倸鍊搁崐宄懊归崶顒夋晪鐟滃酣銆冮妷鈺佺濞达絿鎳撻埀顒冨煐閹便劌顫滈崱妤€鈷掗梺缁樻煥濡瑩骞堥妸銉富閻犲洩寮撴竟鏇熶繆閵堝洤啸闁稿鐩弫鍐Ψ閳轰胶鐣鹃梺鍝勫暙閻楀啴鍩€椤掑﹦鐣垫鐐差儔閹瑧鈧稒顭囪ぐ搴ㄦ⒒閸屾艾鈧绮堟担铏圭濠电姴浼ｅ☉姘磯闁靛ě鍛姸濠电偞鎸婚崺鍐磻閹惧灈鍋撶憴鍕闁挎洏鍨藉畷娲焵?
        
        classStudentMapper.insert(relation);
        
        return "success";
    }
}


