-- ==================== 初始化测试数据（幂等脚本） ====================
-- 说明：MySQL 为持久化数据库，应用每次启动都会执行本脚本；
-- 使用显式主键 + INSERT IGNORE，已存在的数据会自动跳过，不会重复插入。
-- 密码: 启动时自动迁移为BCrypt，默认登录密码: 123456
INSERT IGNORE INTO blog_user (id, username, password, nickname, email, role, status) VALUES
(1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', 'admin@blog.com', 'ADMIN', 1),
(2, 'zhangsan', 'e10adc3949ba59abbe56e057f20f883e', '张三', 'zhangsan@blog.com', 'USER', 1),
(3, 'lisi', 'e10adc3949ba59abbe56e057f20f883e', '李四', 'lisi@blog.com', 'USER', 1);

-- 初始化文章
INSERT IGNORE INTO blog_article (id, title, content, summary, author_id, category, tags, view_count, comment_count, like_count, status) VALUES
(1, 'Spring Boot 4.0 新特性详解', 'Spring Boot 4.0 带来了许多激动人心的新特性。本文将详细介绍最重要的更新内容，包括对 Java 21 虚拟线程的支持、全新的可观测性API、以及改进的GraalVM原生镜像支持等。首先，虚拟线程（Project Loom）的支持让Spring应用在高并发场景下的性能得到了质的飞跃。...', 'Spring Boot 4.0 带来了虚拟线程、可观测性API等重磅新特性', 1, 'Spring', 'SpringBoot,Java,后端', 1250, 15, 88, 1),

(2, 'JWT认证在Spring项目中的最佳实践', 'JSON Web Token（JWT）已经成为现代Web应用中主流的认证方案。本文将分享在Spring Boot项目中集成JWT的最佳实践，包括Token的生成与验证、刷新Token机制、黑名单处理等。我们将从安全性、性能和易用性三个维度来设计一个完善的JWT认证方案。...', '全面讲解JWT在Spring Boot中的集成与最佳实践', 1, 'Spring', 'JWT,安全,认证', 980, 12, 56, 1),

(3, 'Redis缓存策略：从入门到精通', 'Redis作为最流行的缓存中间件，合理使用可以极大提升系统性能。本文将从缓存穿透、缓存击穿、缓存雪崩三大经典问题入手，深入剖析各种缓存策略的原理与实现，包括旁路缓存、读写穿透、异步写回等模式。同时介绍Redis在博客系统中的实际应用场景。...', '深入探讨Redis缓存的各种策略与高并发场景下的应用', 2, '数据库', 'Redis,缓存,高并发', 2100, 25, 120, 1),

(4, 'MyBatis-Plus 高效开发指南', 'MyBatis-Plus 是一款优秀的MyBatis增强工具，在其基础上只做增强不做改变，为简化开发、提高效率而生。本文详细介绍其核心功能：通用CRUD、条件构造器、分页插件、逻辑删除、自动填充等。...', '掌握MyBatis-Plus核心特性，提升开发效率', 2, '数据库', 'MyBatis,ORM,Java', 760, 8, 45, 1),

(5, '构建高并发博客系统架构设计', '如何设计一个能够支撑高并发的博客系统？本文从整体架构出发，涵盖负载均衡、数据库优化、缓存设计、异步处理、限流降级等多个方面。采用Spring Boot + MyBatis-Plus + Redis的技术栈，配合合理的架构设计，轻松应对十万级并发访问。...', '从架构设计到代码实现，全方位构建高并发博客系统', 3, '架构', '架构,高并发,性能优化', 3100, 35, 200, 1);

-- 初始化评论
INSERT IGNORE INTO blog_comment (id, article_id, user_id, parent_id, content, status) VALUES
(1, 1, 2, 0, '写得太好了，非常详细！', 1),
(2, 1, 3, 0, 'Spring Boot 4.0 的虚拟线程确实是一个亮点', 1),
(3, 1, 1, 2, '感谢支持，后续会出更多Spring相关文章', 1),
(4, 3, 1, 0, 'Redis缓存策略讲得很透彻，收获很大', 1),
(5, 5, 2, 0, '架构设计思路很清晰，学习了', 1),
(6, 5, 3, 0, '高并发这块确实需要多实践', 1);

-- ==================== 凡人修仙传·人界篇：修士与功法帖 ====================
-- 主键故意避开小号段（用户 101+ / 文章 1001+ / 评论 1001+），
-- 防止与线上已注册的真实自增 id 撞车——撞上了 INSERT IGNORE 会静默跳过，数据就少了。
-- 密码沿用下方 MD5 占位，启动时由 DataInitializer 统一迁移为 BCrypt，登录密码就是 123456。
-- 正文里的 \n 会被 MySQL 还原成真实换行，文章详情页按 <br> 渲染，所以不需要 markdown 语法。

INSERT IGNORE INTO blog_user (id, username, password, nickname, email, bio, role, status) VALUES
(101, 'hanli',       'e10adc3949ba59abbe56e057f20f883e', '韩立',       'hanli@fanren.dev',       '七玄门出身，四属性伪灵根。掌天瓶在手，步步为营。', 'USER', 1),
(102, 'nangongwan',  'e10adc3949ba59abbe56e057f20f883e', '南宫婉',     'nangongwan@fanren.dev',  '掩月宗弟子。素女元阴功，元阴为本。',                 'USER', 1),
(103, 'modafu',      'e10adc3949ba59abbe56e057f20f883e', '墨大夫',     'modafu@fanren.dev',      '七玄门太医，行医四十年，长春功传人。',               'USER', 1),
(104, 'lifeiyu',     'e10adc3949ba59abbe56e057f20f883e', '厉飞雨',     'lifeiyu@fanren.dev',     '七玄门凡俗武徒，不懂法术，但活得明白。',             'USER', 1),
(105, 'dongxuaner',  'e10adc3949ba59abbe56e057f20f883e', '董萱儿',     'dongxuaner@fanren.dev',  '黄枫谷灵植夫，与药草打交道比与人多。',               'USER', 1),
(106, 'lihuayuan',   'e10adc3949ba59abbe56e057f20f883e', '李化元',     'lihuayuan@fanren.dev',   '黄枫谷长老，筑基期，管过宗门资源分配。',             'USER', 1),
(107, 'dayanshenjun','e10adc3949ba59abbe56e057f20f883e', '大衍神君',   'dayan@fanren.dev',       '上古神念一脉，残魂留传承，只讲神念不讲别的。',       'USER', 1),
(108, 'yinyue',      'e10adc3949ba59abbe56e057f20f883e', '银月',       'yinyue@fanren.dev',      '银月剑器灵。看过的修士比看过的剑多。',               'USER', 1),
(109, 'ziling',      'e10adc3949ba59abbe56e057f20f883e', '紫灵',       'ziling@fanren.dev',      '乱星海行走多年，星宫、散修、海兽都打过交道。',       'USER', 1),
(110, 'bingfeng',    'e10adc3949ba59abbe56e057f20f883e', '冰凤',       'bingfeng@fanren.dev',    '天南冰凤一族，修玄冰真火，正卡在化神那道坎上。',     'USER', 1);

-- 各人一篇主修功法与能力自述
INSERT IGNORE INTO blog_article (id, title, content, summary, author_id, category, tags, view_count, comment_count, like_count, favorite_count, is_top, status) VALUES
(1001, '长春功与青元剑诀：伪灵根的笨功夫',
'在下韩立，越国嘉元郡人氏，七玄门出身，四属性伪灵根——说人话就是资质最差的那一档。\n\n【主修功法】\n一、长春功。墨大夫所传，走的养气一路，进境极慢但根基扎实，不伤经脉。我把它当入门桩功，一直到结丹都没丢。\n二、青元剑诀。木属性剑诀，讲究剑气生发、绵绵不尽，与长春功同属一脉，接得上，不必推倒重来。这是我结丹之后的主攻。\n三、大衍诀。它不攻击，专修识海。斗法时神念先一步探明对方法诀破绽，比多一件法宝都管用。\n\n【能力自述】\n掌天瓶：能催生灵药，把百年份养成千年份。我没有灵丹续命，全靠它把修炼的时间成本压下来。\n青竹蜂云剑：本命飞剑，走的是绵密一路，不刚猛，但耗得起。\n遁光与血影遁：打不过就走，这条不算神通，但救过我最多回。\n\n【给同道的话】\n资质差不是绝路，路线选错才是。我从不修与自己灵根相冲的法诀，也从不接超出自己境界一层的斗法。慢，就是快。',
'七玄门出身、四属性伪灵根，靠长春功打底、青元剑诀入门、大衍诀修神念，把慢功夫走成一条活路。', 101, '功法', '长春功,青元剑诀,大衍诀,掌天瓶', 2680, 4, 186, 92, 1, 1),

(1002, '素女元阴功：元阴为本，不在双修',
'掩月宗在册弟子南宫婉，此处只讲本门功法，不谈宗门私事。\n\n【功法根基】\n素女元阴功以「元阴不泄」为第一戒。修的是自身一气，不是别人的道侣。世人拿这门功法和双修之术混为一谈，是认错了本。元阴是蓄，不是换。\n\n【行功次第】\n一、静中养气。子时面北静坐，呼吸细长到自己能听见，气走任脉而不散。\n二、月华洗髓。每半月取一次月满之夜，让灵气随潮信入体，这一步最忌心浮气躁，躁则寒气凝在经脉里，日后要还。\n三、凝气成刃。元阴功的杀伐手段只有一式，寒气凝刃，出手极冷极净，不留余地，所以非必要不出。\n\n【能力与短长】\n长处在绵长与洁净：真元损耗慢、伤势不易恶化、对邪毒一门天然有几分压制。短处在爆发，同境界硬拼刚猛路数，我多半讨不了好。\n\n【一句劝】\n这门功夫最难的不是行气，是戒律。守不住清静的人，练得越快，反噬越早。',
'掩月宗素女元阴功的行功次第与戒律：元阴是蓄，不是换，最难的不是行气而是守住清静。', 102, '功法', '素女元阴功,掩月宗,月华洗髓', 1740, 2, 121, 58, 0, 1),

(1003, '夺舍三忌：我在七玄门行医的四十年',
'老朽墨居仁，七玄门太医，门中人唤我墨大夫。行医四十年，也修了四十年长春功。\n\n【我这一门的底子】\n长春功是养气延年的路数，进境慢得看不出，但能压住伤病、拖住大限。老朽卡在炼气后期二十年，寿数将尽，这话如今说出来也不害臊。\n\n【夺舍三忌】\n一忌根器不合。神魂迁入与自身灵根相冲的皮囊，第一年看着无事，第三年真元自己就乱了。\n二忌因果不清。被夺之身往往带着旧账，师门、道侣、仇家，一样都跑不掉，你接了他的人身，就接了他的劫。\n三忌心气不平。行此术者，多半是自己不肯认输。一个连自己寿数都认不下的人，凭什么驾驭别人的一生。\n\n【我最后悔的事】\n传功给那孩子的时候，我只当他是容器。他比我沉得住气，也比我记得牢。今日把这三忌写出来，是劝后来人，也是认自己的账。\n\n功法无罪，用功法的人心术不正，才是祸。',
'七玄门太医自述：长春功养气延年的底子，以及卡在炼气后期二十年、寿数将尽之后悟出的夺舍三忌。', 103, '功法', '长春功,夺舍,七玄门', 1320, 2, 96, 41, 0, 1),

(1004, '八极拳与辟谷丹：凡人站在修士脚边怎么活',
'我叫厉飞雨，七玄门凡俗武徒，没有灵根，练的是拳脚。这篇帖子大概是人界篇里最不像修仙的一份心得。\n\n【凡人的功夫】\n八极拳讲「崩、撼、突、击」，一寸之内发力。放在修士面前，一掌就能被护身灵光弹开。但我练了十年，好处是实打实的：跑得快、扛得住、受伤了能自己走回去。\n\n【辟谷丹到底是什么】\n门里发过一枚，据说是一位仙师赏的。含一粒在嘴里，三天不饿。老实说：不难吃，也不好吃，舌根发麻，喝完要灌半囊水。\n它不是丹药，顶多算个干粮。真正的丹药有灵气，吃了能行气，凡人吃了要出事——我见过门里师兄偷服一枚凝气散，七窍流血躺了半月。\n\n【凡人在修仙界的活法】\n一、别替仙师扛事。他们口中的一句「事后必有重谢」，从来不是给你的。\n二、别问不该问的。山里的动静、夜里的剑光，看见也当没看见。\n三、把命放在自己脚上。修士靠遁光，我只能靠这两条腿，所以我的路要自己先趟一遍。\n\n后来我那位同门兄弟真成了大事。他教我一句话：能活着走到最后的，都不是最能打的。',
'一个没有灵根的凡俗武徒，聊八极拳、辟谷丹的真话，以及修士脚边三条保命规矩。', 104, '丹药', '辟谷丹,八极拳,七玄门', 980, 2, 74, 22, 0, 1),

(1005, '黄枫谷灵植手记：一株药草的三年',
'黄枫谷灵植弟子董萱儿。宗门药园的活计，我干了六年。\n\n【灵植夫的日常】\n卯时入园，先看露、再看土色。灵草不是浇水就能活的：青芝怕闷，地龙草怕亮，宁神花要在人少的时候才肯抽穗。药园一天要转三遍，少一遍第二天就给你脸色看。\n\n【木属性功法在这里怎么用】\n我修的宗门《青木吐纳诀》没什么杀伐本事，唯一长处是能把自己的木灵气渡进根须。催芽、接骨、救一株已经半枯的老参，靠的都是这个。代价是渡一次元气要虚两天，所以宗门算得精：灵植夫的修炼资源给得薄，因为我们的灵气大半给了药。\n\n【一份账】\n一株百年青芝：育苗四月、养土九月、看火十一月，前后两年多，中间一次潮气没弄对就全废。\n而市面上百年灵药为什么贵？贵在这两年里，有个人什么都不能干，只能看着它。\n\n【给采药人的提醒】\n挖野参留三分根，别断脉。山里的药是养出来的，不是抢出来的。这一片挖秃了，下一代灵植夫就没得种了。',
'黄枫谷药园六年的灵植夫手记：青芝怕闷、地龙草怕亮，以及一株百年灵药背后两年的死盯。', 105, '灵植', '灵植,青木吐纳诀,药园', 1130, 1, 63, 28, 0, 1),

(1006, '筑基期的资源账：宗门与散修差在哪',
'在下李化元，黄枫谷执事长老，筑基期多年。今日不谈法诀，谈账。\n\n【一名宗门筑基弟子每年拿到什么】\n灵石四十枚、引气丹六瓶、洞府一间、宗门功法两部、犯事时还有长老替你说话。\n\n【同样资质的散修要付出什么】\n灵石自己挖，丹药自己炼（炸一炉赔一炉），洞府自己抢，功法拿命去换。最贵的是情报：哪处遗迹开了、哪位前辈坐化了，宗门弟子第二天就知道，散修第三年才传闻到耳朵里。\n\n【筑基丹为什么贵】\n三味主药都难。年份要够、人要守着炼、成丹率不到三成。一炉下去，一家宗门半年积蓄。所以筑基丹从来不是商品，是分配权。宗门拿它卡住弟子，散修拿它卖一辈子命。\n\n【几句不中听的话】\n散修骂宗门小气，宗门骂散修不守规矩，两边都没说错。资源就那么多，修到高处的人把梯子抽了，因为当年他们也是踩着别人上来的。\n\n我年轻时也不服。如今执了这杆笔，也只是把同样的事往下传。',
'一名宗门长老把筑基期的资源分配摊开算：灵石、丹药、洞府、情报，以及筑基丹背后真正的分配权。', 106, '宗门', '筑基,资源,黄枫谷', 1560, 2, 88, 46, 0, 1),

(1007, '大衍诀：神念分识与识海之宽',
'老夫大衍氏。肉身早没于上古一战，此篇借残魂所留传承而书，不记其他。\n\n【神念是什么】\n灵气在外，神念在内。修到深处，一念可探百丈，可御一物，可在一息之间分出三缕。\n\n【大衍诀三步】\n一、定识。先能把念头按住。多数人连十息都定不住，就想着分识，那是自寻死路。\n二、分识。一念化为二，如两枝同生一干。分得开，还要合得拢，合不拢就是疯。\n三、千识齐动。到这一步，一人可同御数十件法器，也可在斗法时同时算住对方三处破绽。\n\n【代价】\n分识每多一缕，识海便窄一分。识海窄了，容纳不下自己的本命神识，轻则记不清旧事，重则分不清今昔。老夫一生最得意的本事与最惨的教训，都是同一件。\n\n【一句断语】\n神念是刀，能开法诀破绽，也能剔去自己的神魂。修此诀前，先想清楚你要多出来的那几缕，准备拿什么换。',
'上古神念一脉的传承自述：定识、分识、千识齐动三步，以及每多一缕分识、识海便窄一分的代价。', 107, '神通', '大衍诀,神念,识海', 2040, 1, 143, 77, 0, 1),

(1008, '器灵视角：法宝祭炼与主人心性',
'我名银月，本为剑中器灵。以下所述，全是从剑里看见的东西。\n\n【祭炼三段】\n一、认主。血炼也好、真元浸养也好，这一段法宝只是器物，它记住的是你的气息。\n二、通灵。法器与你神识相接，你心念一动它先知道。多数修士一辈子停在第一段，他们的法宝到死都不认得他。\n三、化形。器灵自生，法宝开始挑主人。到这一步，谁祭炼谁都难说。\n\n【器灵最看重主人什么】\n不是资质，不是境界，是心性。\n你拿它护身，它就稳。你拿它逞凶，它就躁。你心虚的一刻，剑锋会慢上半分——那半分就是你能不能活着回来的区别。\n\n【修士常犯的三个错】\n一、只养不战。法宝长期不见血，神识相接时会生涩，关键时刻脱手。\n二、频繁换宝。每换一件都要重头认主，看着实力涨了，实则器灵一段都没养起来。\n三、把法宝当替死鬼。真到了该你挡的一剑，你退了，从此这件法宝也不会再替你。\n\n【一句话】\n法宝是修士的另一条命。你把命交出去之前，先想清楚自己配不配拿回来。',
'一把剑里的器灵写的祭炼三段：认主、通灵、化形，以及器灵真正看重的不是资质而是心性。', 108, '法宝', '银月剑,器灵,祭炼', 1820, 1, 132, 65, 0, 1),

(1009, '乱星海生存录：星宫、散修与海兽潮汐',
'紫灵。在乱星海走了这些年，写一份能救命的东西。\n\n【这片海怎么排座次】\n乱星海不认宗门辈分，只认两件事：你背后有没有势力，你手上有没有货。\n星宫一系占着最好的几处灵岛，掌潮汛与航道，收的是过路的钱。世家靠阵法与海兽皮骨起家。剩下的大半，都是散修，结队出海，分账时最易翻脸。\n\n【海兽潮汐】\n每逢潮信，深海兽群上涌，看着是劫，也是钱。老手与新手的差别不在能打多少，而在什么时候退：风色一变、海鸟低飞、水下泛凉，三条中两条就必须走。\n我见过一整队人因为多追了一头二阶海兽，第二日那片海上连一块船板都没剩下。\n\n【出海四条规矩】\n一、不接不认得路的海图。\n二、不在陌生灵岛上先动手。\n三、分账先说，后头就不必吵。\n四、留一条退路的灵石，谁都不许碰。\n\n【给初来的人的一句】\n这里的人笑起来都很客气。你要看的不是他笑得暖不暖，而是他的手放在哪里。',
'乱星海的座次、海兽潮汐与出海四条规矩：老手与新手的差别不在能打多少，而在什么时候退。', 109, '见闻', '乱星海,星宫,海兽潮汐', 1610, 1, 108, 51, 0, 1),

(1010, '玄冰真火：化神期那道天堑',
'天南冰凤氏。元婴后期，卡在化神已十一年。\n\n【本命神通】\n玄冰真火名里有火，实则至寒。它烧的不是躯体，是水气与灵气之间的牵连——被它沾上，真元自己就会冻住，运不开了。\n一族之中，此火能引几分全看血脉，旁人学不去。我年轻时以此自负，后来才明白：本命神通是起点，不是答案。\n\n【元婴与化神差在哪】\n元婴以下，你在用灵气。化神，你要能叫得动天地。\n这不是法力量的差别，是「懂不懂法则」的差别。懂了，一息之间天地之气为你所用，不必从自己身上取。不懂，修到寿元尽头也还在门外。\n\n【卡关的十一年】\n我试过三次闭死关。前两次都失败在同一个地方：我太想赢。\n天地法则不吃这一套。它不看你多强，只看你能不能静到与它同频。第三次我什么都不做，在雪地里坐了两年，看雪落在地上不化——那一下才真正推开一条缝。\n\n【一句话】\n化神不是冲过去的，是你不再想冲的时候，它自己开的。',
'天南冰凤一族的玄冰真火，以及卡在化神十一年悟出的事：天地法则不看你多强，只看你能不能静到与它同频。', 110, '功法', '玄冰真火,化神,天南', 2230, 1, 167, 84, 0, 1);

-- 修士之间的互评，让帖子像论坛而不是一人博客（comment_count 已与下面条数对齐）
INSERT IGNORE INTO blog_comment (id, article_id, user_id, parent_id, content, status) VALUES
(1001, 1001, 103, 0, '能记住自己从哪来的，不多。你比我有出息。', 1),
(1002, 1001, 107, 0, '大衍诀到我手里只留下三步，你能靠它活到今日，说明你没照着练。', 1),
(1003, 1001, 104, 0, '韩兄弟这篇写得像个大人物了。当年在七玄门，你连肉干都舍不得多吃一块。', 1),
(1004, 1001, 108, 0, '作为你剑里的人说一句：你最大的本事不是功法，是跑得比别人快。', 1),
(1005, 1002, 101, 0, '月华洗髓那一步，若寒气凝在经脉里，可用长春功缓缓引开，不必硬压。', 1),
(1006, 1002, 105, 0, '药园里有三味辅草能缓那分寒性，前辈若要，我留一份。', 1),
(1007, 1003, 104, 0, '先生当年在门中施药，确实救过不少人。这三忌，我替凡俗弟子记下了。', 1),
(1008, 1003, 101, 0, '传功之恩，韩立不敢忘。三忌之言，受用至今。', 1),
(1009, 1004, 101, 0, '辟谷丹的话你没说错。凡人那条路，比我难走。', 1),
(1010, 1004, 103, 0, '凝气散那件事，门里三人都是我用针吊回来的。凡人不可强修，这是实话。', 1),
(1011, 1005, 106, 0, '宗门欠灵植夫的那份账，今日被你说出来了。', 1),
(1012, 1006, 109, 0, '乱星海也一样。只不过这里不叫宗门，叫世家。', 1),
(1013, 1006, 101, 0, '长老这笔账算得明白。只是当年在黄枫谷，我可没拿到这四十枚灵石。', 1),
(1014, 1007, 101, 0, '靠此诀活了至今。分识的代价，我见过，也付过。', 1),
(1015, 1008, 101, 0, '剑里看人，比人自己看自己清楚。', 1),
(1016, 1009, 110, 0, '乱星海的水太浑。天南的雪干净些，可惜也冷。', 1),
(1017, 1010, 109, 0, '看了两遍。雪落在地上不化——这一句我拿走了。', 1);

-- ==================== 人界篇第二批：乱星海、天南、大晋与凡俗 ====================
-- 依旧男女各半（111-120 为男修、121-130 为女修），id 继续往后排，不碰真实自增段。
INSERT IGNORE INTO blog_user (id, username, password, nickname, email, bio, avatar, role, status) VALUES
(111, 'manhuzi',      'e10adc3949ba59abbe56e057f20f883e', '蛮胡子',     'manhuzi@fanren.dev',      '乱星海元婴中期，托塔天魔功，体修。黄须卷曲，蓝袍。',       '/images/avatars/manhuzi.svg',      'USER', 1),
(112, 'quhun',        'e10adc3949ba59abbe56e057f20f883e', '曲魂',       'quhun@fanren.dev',        '本名张铁，七玄门出身。象甲功炼体，如今算不算是人都两说。', '/images/avatars/quhun.svg',        'USER', 1),
(113, 'jiyinzushi',   'e10adc3949ba59abbe56e057f20f883e', '极阴祖师',   'jiyinzushi@fanren.dev',   '乱星海元婴，摄魂擒婴，虚天鼎一事至今未了。',             '/images/avatars/jiyinzushi.svg',   'USER', 1),
(114, 'liudaojisheng','e10adc3949ba59abbe56e057f20f883e', '六道极圣',   'liudao@fanren.dev',       '魔道六宗出身。不谈来历，只谈功法。',                     '/images/avatars/liudaojisheng.svg','USER', 1),
(115, 'wantianming',  'e10adc3949ba59abbe56e057f20f883e', '万天明',     'wantianming@fanren.dev',  '正道万氏子弟。鉴宝三十年，看走眼一次。',                 '/images/avatars/wantianming.svg',  'USER', 1),
(116, 'jinkui',       'e10adc3949ba59abbe56e057f20f883e', '金魁',       'jinkui@fanren.dev',       '星宫大长老，元婴中期巅峰。执律令，镇海疆。',             '/images/avatars/jinkui.svg',       'USER', 1),
(117, 'lingxiaofeng', 'e10adc3949ba59abbe56e057f20f883e', '凌啸风',     'lingxiaofeng@fanren.dev', '天星双圣之一。元磁神光，与夫人同修三百年。',             '/images/avatars/lingxiaofeng.svg', 'USER', 1),
(118, 'xiangzhili',   'e10adc3949ba59abbe56e057f20f883e', '向之礼',     'xiangzhili@fanren.dev',   '化神初期，人界顶点。三千年没等到飞升。',                 '/images/avatars/xiangzhili.svg',   'USER', 1),
(119, 'yunlulaomo',   'e10adc3949ba59abbe56e057f20f883e', '云露老魔',   'yunlu@fanren.dev',        '魔道修士，云露魔光。有个不认我的女儿。',                 '/images/avatars/yunlulaomo.svg',   'USER', 1),
(120, 'sunergou',     'e10adc3949ba59abbe56e057f20f883e', '孙二狗',     'sunergou@fanren.dev',     '四平帮帮主，凡俗之人。替仙师看着一具炼尸。',             '/images/avatars/sunergou.svg',     'USER', 1),
(121, 'chenqiaoqian', 'e10adc3949ba59abbe56e057f20f883e', '陈巧倩',     'chenqiaoqian@fanren.dev', '黄枫谷弟子，翠云诀。宗门里的女修怎么活，我说几句。',     '/images/avatars/chenqiaoqian.svg', 'USER', 1),
(122, 'mocaihuan',    'e10adc3949ba59abbe56e057f20f883e', '墨彩环',     'mocaihuan@fanren.dev',    '墨家三女，没有灵根。医道我照样学得下来。',               '/images/avatars/mocaihuan.svg',    'USER', 1),
(123, 'hanyunzhi',    'e10adc3949ba59abbe56e057f20f883e', '菡云芝',     'hanyunzhi@fanren.dev',    '灵兽山一脉。太南庙会那年的糖人，我还记得。',             '/images/avatars/hanyunzhi.svg',    'USER', 1),
(124, 'mupeiling',    'e10adc3949ba59abbe56e057f20f883e', '慕沛灵',     'mupeiling@fanren.dev',    '有夫妻之名、无夫妻之实。分寸二字，我练得比功法熟。',     '/images/avatars/mupeiling.svg',    'USER', 1),
(125, 'caomengrong',  'e10adc3949ba59abbe56e057f20f883e', '曹梦容',     'caomengrong@fanren.dev',  '大晋坊市丹药铺。真假我一眼能分。',                       '/images/avatars/caomengrong.svg',  'USER', 1),
(126, 'guiling',      'e10adc3949ba59abbe56e057f20f883e', '圭灵',       'guiling@fanren.dev',      '昆吾山玄岩龟，十级妖兽。守山八百年，壳比话多。',         '/images/avatars/guiling.svg',      'USER', 1),
(127, 'liuleer',      'e10adc3949ba59abbe56e057f20f883e', '柳乐儿',     'liuleer@fanren.dev',      '灵寰界长大，九尾一脉如今归我管。',                       '/images/avatars/liuleer.svg',      'USER', 1),
(128, 'baifengxianzi','e10adc3949ba59abbe56e057f20f883e', '白凤仙子',   'baifeng@fanren.dev',      '落云宗白凤峰峰主，元婴初期。第一美女是虚名，剑是实的。', '/images/avatars/baifengxianzi.svg','USER', 1),
(129, 'wansangu',     'e10adc3949ba59abbe56e057f20f883e', '万三姑',     'wansangu@fanren.dev',     '正道第一人。位置越高，能出手的事越少。',                 '/images/avatars/wansangu.svg',     'USER', 1),
(130, 'hongfu',       'e10adc3949ba59abbe56e057f20f883e', '红拂',       'hongfu@fanren.dev',       '红拂三剑。当年能带走女儿，如今也护得住。',               '/images/avatars/hongfu.svg',       'USER', 1);

-- ==================== 人界篇第三批：有名有姓的元婴老怪与妖修 ====================
-- 这一批只为榜单与斗法台建身份（昵称、一句话来历、主页可用），不发文也不互评：
-- 站内设定表（CultivationCatalog 131-173）才是他们的正文。INSERT IGNORE 保证每次启动重跑都不冲突。
INSERT IGNORE INTO blog_user (id, username, password, nickname, email, bio, avatar, role, status) VALUES
(131, 'weiwuya',        'e10adc3949ba59abbe56e057f20f883e', '魏无涯',     'weiwuya@fanren.dev',        '天南九国盟化意门太上长老，元婴后期巅峰。十绝毒，毒道冠绝天南。',       '/images/default-avatar.svg', 'USER', 1),
(132, 'zhiyangshangren','e10adc3949ba59abbe56e057f20f883e', '至阳上人',   'zhiyang@fanren.dev',        '太真门太上长老，天南正道第一人。静心诀，至阳真火。',                 '/images/default-avatar.svg', 'USER', 1),
(133, 'hehuanlaomo',    'e10adc3949ba59abbe56e057f20f883e', '合欢老魔',   'hehuan@fanren.dev',         '合欢宗大长老，天南第一魔修。不谈正邪，只谈效果。',                   '/images/default-avatar.svg', 'USER', 1),
(134, 'tianhenlaoguai', 'e10adc3949ba59abbe56e057f20f883e', '天恨老怪',   'tianhen@fanren.dev',        '天南魔道散修，三大修士之下第一人。曾轻伤至阳上人。',               '/images/default-avatar.svg', 'USER', 1),
(135, 'longhan',        'e10adc3949ba59abbe56e057f20f883e', '龙晗',       'longhan@fanren.dev',        '鸾鸣宗太上长老，天道盟领军。领军是最后退的那个。',                   '/images/default-avatar.svg', 'USER', 1),
(136, 'fengbing',       'e10adc3949ba59abbe56e057f20f883e', '凤冰',       'fengbing@fanren.dev',       '鸾鸣宗，龙晗道侣。他出头，我算账。',                                 '/images/default-avatar.svg', 'USER', 1),
(137, 'jinwuhuan',      'e10adc3949ba59abbe56e057f20f883e', '金武环',     'jinwuhuan@fanren.dev',      '古剑门大长老，化剑为丝。与自己切磋过的那位，赢在翅上。',           '/images/default-avatar.svg', 'USER', 1),
(138, 'nangongque',     'e10adc3949ba59abbe56e057f20f883e', '南宫阙',     'nangongque@fanren.dev',     '掩月宗元婴剑修，持凝光镜。同为南宫，与婉妹不熟。',                 '/images/default-avatar.svg', 'USER', 1),
(139, 'linghulaoguai',  'e10adc3949ba59abbe56e057f20f883e', '令狐老怪',   'linghu@fanren.dev',         '黄枫谷唯一的元婴。谷里弟子叫我老祖。',                               '/images/default-avatar.svg', 'USER', 1),
(140, 'nanlonghou',     'e10adc3949ba59abbe56e057f20f883e', '南陇侯',     'nanlonghou@fanren.dev',     '天南散修，万尺一线。退路就是我的宗门。',                             '/images/default-avatar.svg', 'USER', 1),
(141, 'suihunzhenren',  'e10adc3949ba59abbe56e057f20f883e', '碎魂真人',   'suihun@fanren.dev',         '天南散修，碎魂大法。人身好护，魂难护。',                             '/images/default-avatar.svg', 'USER', 1),
(142, 'luweiying',      'e10adc3949ba59abbe56e057f20f883e', '鲁卫英',     'luweiying@fanren.dev',      '天极门长老。坠魔谷那一役，我算没回去的那一个。',                   '/images/default-avatar.svg', 'USER', 1),
(143, 'dongmentu',      'e10adc3949ba59abbe56e057f20f883e', '东门图',     'dongmentu@fanren.dev',      '御灵宗大长老。兽不听我的，只听划算。',                               '/images/default-avatar.svg', 'USER', 1),
(144, 'huolongtongzi',  'e10adc3949ba59abbe56e057f20f883e', '火龙童子',   'huolong@fanren.dev',        '天南魔道散修，火龙真炎。我叫童子，因为这张脸不会老。',           '/images/default-avatar.svg', 'USER', 1),
(145, 'baiyaoyi',       'e10adc3949ba59abbe56e057f20f883e', '白瑶怡',     'baiyaoyi@fanren.dev',       '天南顶尖元中女修，白虹一剑。独斗过十级妖狼。',                       '/images/default-avatar.svg', 'USER', 1),
(146, 'chengtiankun',   'e10adc3949ba59abbe56e057f20f883e', '程天坤',     'chengtiankun@fanren.dev',   '落云宗掌门。掌门不是最强的，是最后一个走的。',                       '/images/default-avatar.svg', 'USER', 1),
(147, 'lvluo',          'e10adc3949ba59abbe56e057f20f883e', '吕洛',       'lvluo@fanren.dev',          '落云宗长老，千浪决。浪多不代表浪大，靠的是打不完。',                 '/images/default-avatar.svg', 'USER', 1),
(148, 'wenqing',        'e10adc3949ba59abbe56e057f20f883e', '温青',       'wenqing@fanren.dev',       '天星双圣之一，天灵根。元磁神光两极之中她养阳极。',                 '/images/default-avatar.svg', 'USER', 1),
(149, 'fangzongshi',    'e10adc3949ba59abbe56e057f20f883e', '房宗主',     'fangzongshi@fanren.dev',    '大晋阴罗宗宗主，三具尸王随身。灭仙珠还在匣子里。',                 '/images/default-avatar.svg', 'USER', 1),
(150, 'qianlaomo',      'e10adc3949ba59abbe56e057f20f883e', '乾老魔',     'qianlaomo@fanren.dev',      '大晋阴罗宗大长老，逆情断天大法。五子同心魔，合体不灭火。',         '/images/default-avatar.svg', 'USER', 1),
(151, 'xuanqingzi',     'e10adc3949ba59abbe56e057f20f883e', '玄青子',     'xuanqingzi@fanren.dev',     '大晋太一门话事人。灵宝不是我们的，我们只替它看门。',               '/images/default-avatar.svg', 'USER', 1),
(152, 'qimiaozhenren',  'e10adc3949ba59abbe56e057f20f883e', '七妙真人',   'qimiao@fanren.dev',         '大晋天魔宗执法长老，赤鸣鼓。一鼓破胆，再鼓散魂。',                   '/images/default-avatar.svg', 'USER', 1),
(153, 'kunwuji',        'e10adc3949ba59abbe56e057f20f883e', '坤无极',     'kunwuji@fanren.dev',        '大晋血凄门大长老。别人炼气，我炼血。',                               '/images/default-avatar.svg', 'USER', 1),
(154, 'yuanzhidashi',   'e10adc3949ba59abbe56e057f20f883e', '元智大师',   'yuanzhi@fanren.dev',        '大晋雷音宗，明王诀与四层金刚诀。肉身堪比法宝。',                   '/images/default-avatar.svg', 'USER', 1),
(155, 'biyuechanshi',   'e10adc3949ba59abbe56e057f20f883e', '碧月禅师',   'biyue@fanren.dev',          '大晋净火宗，四百年入元后。没人见过我出手。',                       '/images/default-avatar.svg', 'USER', 1),
(156, 'yixitian',       'e10adc3949ba59abbe56e057f20f883e', '易洗天',     'yixitian@fanren.dev',       '大晋四大散修之一，曾轻斩八级妖兽。靠山是那一剑。',                 '/images/default-avatar.svg', 'USER', 1),
(157, 'hanlishangren',  'e10adc3949ba59abbe56e057f20f883e', '寒骊上人',   'hanli@fanren.dev',          '大晋北夜小极宫，两口乾蓝鼎，乾蓝冰焰修到极致。',                   '/images/default-avatar.svg', 'USER', 1),
(158, 'xiaojiugongzhu', 'e10adc3949ba59abbe56e057f20f883e', '小极宫宫主', 'xiaojiugong@fanren.dev',    '北夜小极宫主。手里有一把黄泉的钥匙。',                               '/images/default-avatar.svg', 'USER', 1),
(159, 'huangguiguimu',  'e10adc3949ba59abbe56e057f20f883e', '黄泉鬼母',   'huanggui@fanren.dev',       '北夜黄泉白板鬼修。最不值钱的就是那具身子，它早烂了。',               '/images/default-avatar.svg', 'USER', 1),
(160, 'yejiadalao',     'e10adc3949ba59abbe56e057f20f883e', '叶家大长老', 'yejiadalao@fanren.dev',     '大晋叶家大长老，黑血刃。世家靠的是肯把精血拿出来的人多。',           '/images/default-avatar.svg', 'USER', 1),
(161, 'yeyuesheng',     'e10adc3949ba59abbe56e057f20f883e', '叶月圣',     'yeyuesheng@fanren.dev',      '叶家七叔。族里都当我修为平平 —— 藏了三百年。',                       '/images/default-avatar.svg', 'USER', 1),
(162, 'fucheng',        'e10adc3949ba59abbe56e057f20f883e', '富成',       'fucheng@fanren.dev',        '大晋九幽宗执法长老。宗门里的事，办完了不必说。',                   '/images/default-avatar.svg', 'USER', 1),
(163, 'xudaxianshi',    'e10adc3949ba59abbe56e057f20f883e', '徐大仙师',   'xudaxianshi@fanren.dev',    '天澜草原大仙师，与至阳上人、乾老魔齐名。草原认祭坛。',             '/images/default-avatar.svg', 'USER', 1),
(164, 'tianchui',       'e10adc3949ba59abbe56e057f20f883e', '田锤',       'tianchui@fanren.dev',       '慕兰草原第四神师，刚进阶元后。神来那一瞬，我不是我。',             '/images/default-avatar.svg', 'USER', 1),
(165, 'jinjiaowang',    'e10adc3949ba59abbe56e057f20f883e', '金蛟王',     'jinjiao@fanren.dev',        '乱星海外海蛟龙族长，十级妖修。外海只认鳞片的硬度。',                 '/images/default-avatar.svg', 'USER', 1),
(166, 'wannianshixiong','e10adc3949ba59abbe56e057f20f883e', '万年尸熊',   'shixiong@fanren.dev',       '万妖谷副谷主，持血刃。昆吾山那一夜我没让过路。',                       '/images/default-avatar.svg', 'USER', 1),
(167, 'qingbeicanglang','e10adc3949ba59abbe56e057f20f883e', '青背苍狼',   'canglang@fanren.dev',       '万妖谷副谷主。我穿人衣、说人话，只为叫人先不想动手。',             '/images/default-avatar.svg', 'USER', 1),
(168, 'mukui',          'e10adc3949ba59abbe56e057f20f883e', '木魁',       'mukui@fanren.dev',          '昆吾山万年古木。我不追人，我长得比你跑得远。',                       '/images/default-avatar.svg', 'USER', 1),
(169, 'yinchiyecha',    'e10adc3949ba59abbe56e057f20f883e', '银翅夜叉',   'yecha@fanren.dev',          '昆吾山四妖，仅次于金身月尸的十级飞天尸。想事的尸最难杀。',         '/images/default-avatar.svg', 'USER', 1),
(170, 'shiqinshou',     'e10adc3949ba59abbe56e057f20f883e', '狮禽兽',     'shiqin@fanren.dev',         '昆吾山四妖，狮首鹰身四翅，未化形。我不会说话。',                   '/images/default-avatar.svg', 'USER', 1),
(171, 'chelaoyao',      'e10adc3949ba59abbe56e057f20f883e', '车老妖',     'chelaoyao@fanren.dev',      '万妖幡主。幡里那些东西都欠我，欠久了就肯替我死。',                   '/images/default-avatar.svg', 'USER', 1),
(172, 'xuangushangren', 'e10adc3949ba59abbe56e057f20f883e', '玄骨上人',   'xuangushangren@fanren.dev', '乱星海虚天殿旧怪，肉身被打爆、元婴被擒，多年后复出。死过一次，什么都不必要了。', '/images/default-avatar.svg', 'USER', 1),
(173, 'wentianren',     'e10adc3949ba59abbe56e057f20f883e', '温天仁',     'wentianren@fanren.dev',     '乱星海结丹修士第一人，六道极圣门下。一身宝物，一身劫。',         '/images/default-avatar.svg', 'USER', 1);

-- 线上 id=131 曾被一个占位空账号占住（昵称就是「无名之辈」，无文章、无评论、无操作日志），
-- 而 INSERT IGNORE 碰到已存在的 id 不会改写，榜单里那位化意门太上长老就会顶着一个占位名字。
-- 这条只在昵称仍是那个占位值时动手，真实注册用户一概不碰；改完条件就不成立了，每次启动重跑幂等。
UPDATE blog_user SET username = 'weiwuya', nickname = '魏无涯', email = 'weiwuya@fanren.dev',
                     bio = '天南九国盟化意门太上长老，元婴后期巅峰。十绝毒，毒道冠绝天南。',
                     avatar = '/images/default-avatar.svg'
WHERE id = 131 AND nickname = '无名之辈';

-- 前一批十位角色的头像：只补空值和旧的统一占位图（default-avatar），
-- 用户自己上传过的（/uploads/avatars/...）一概不动；本脚本每次启动都跑，必须幂等。
UPDATE blog_user SET avatar = '/images/avatars/hanli.svg'        WHERE id = 101 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/nangongwan.svg'   WHERE id = 102 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/modafu.svg'       WHERE id = 103 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/lifeiyu.svg'      WHERE id = 104 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/dongxuaner.svg'   WHERE id = 105 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/lihuayuan.svg'    WHERE id = 106 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/dayanshenjun.svg' WHERE id = 107 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/yinyue.svg'       WHERE id = 108 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/ziling.svg'       WHERE id = 109 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/bingfeng.svg'     WHERE id = 110 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');

-- 第三批 43 位元婴/妖修（131-173）的文字头像：同样只补仍是默认占位图的行，用户自己上传过的一概不动。
-- 这批头像文件与 101-110 那批同审美（渐变圆 + 姓氏首字），生成后放在 /images/avatars/ 下。
UPDATE blog_user SET avatar = '/images/avatars/weiwuya.svg'          WHERE id = 131 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/zhiyangshangren.svg'  WHERE id = 132 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/hehuanlaomo.svg'      WHERE id = 133 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/tianhenlaoguai.svg'   WHERE id = 134 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/longhan.svg'          WHERE id = 135 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/fengbing.svg'         WHERE id = 136 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/jinwuhuan.svg'        WHERE id = 137 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/nangongque.svg'       WHERE id = 138 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/linghulaoguai.svg'    WHERE id = 139 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/nanlonghou.svg'       WHERE id = 140 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/suihunzhenren.svg'    WHERE id = 141 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/luweiying.svg'        WHERE id = 142 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/dongmentu.svg'        WHERE id = 143 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/huolongtongzi.svg'    WHERE id = 144 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/baiyaoyi.svg'         WHERE id = 145 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/chengtiankun.svg'     WHERE id = 146 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/lvluo.svg'            WHERE id = 147 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/wenqing.svg'          WHERE id = 148 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/fangzongshi.svg'      WHERE id = 149 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/qianlaomo.svg'        WHERE id = 150 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/xuanqingzi.svg'       WHERE id = 151 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/qimiaozhenren.svg'    WHERE id = 152 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/kunwuji.svg'          WHERE id = 153 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/yuanzhidashi.svg'     WHERE id = 154 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/biyuechanshi.svg'     WHERE id = 155 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/yixitian.svg'         WHERE id = 156 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/hanlishangren.svg'    WHERE id = 157 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/xiaojiugongzhu.svg'   WHERE id = 158 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/huangguiguimu.svg'    WHERE id = 159 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/yejiadalao.svg'       WHERE id = 160 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/yeyuesheng.svg'       WHERE id = 161 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/fucheng.svg'          WHERE id = 162 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/xudaxianshi.svg'      WHERE id = 163 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/tianchui.svg'         WHERE id = 164 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/jinjiaowang.svg'      WHERE id = 165 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/wannianshixiong.svg'  WHERE id = 166 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/qingbeicanglang.svg'  WHERE id = 167 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/mukui.svg'            WHERE id = 168 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/yinchiyecha.svg'      WHERE id = 169 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/shiqinshou.svg'       WHERE id = 170 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/chelaoyao.svg'        WHERE id = 171 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/xuangushangren.svg'   WHERE id = 172 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');
UPDATE blog_user SET avatar = '/images/avatars/wentianren.svg'       WHERE id = 173 AND (avatar IS NULL OR avatar = '' OR avatar LIKE '%default-avatar%');

-- 静态头像改版（主字放大到 64 并居中、去掉右下角 emoji 徽标）：文件内容变了但 URL 没变，
-- 而 Nginx 对 /images/ 下发的是 max-age=2592000, immutable，浏览器根本不会回来重新取，
-- 老用户看着还是旧字号——这正是「只有新注册的改过来了」的另一半原因。
-- 统一给这批 SVG 加版本号查询串强制失效；用户上传的 /uploads/avatars/... 不带 .svg 不会命中，
-- 已带版本号的也不再重复处理，本脚本每次启动都跑，幂等。
UPDATE blog_user SET avatar = CONCAT(avatar, '?v=2')
WHERE avatar LIKE '/images/avatars/%.svg' AND avatar NOT LIKE '%?v=%';

-- 第二批功法自述：先男修十篇（1011-1020）
INSERT IGNORE INTO blog_article (id, title, content, summary, author_id, category, tags, view_count, comment_count, like_count, favorite_count, is_top, status) VALUES
(1011, '托塔天魔功：乱星海第一防御魔功是怎么练成的',
'老夫蛮胡子，乱星海一个元婴老怪，黄须卷曲，穿蓝袍。今日高兴，说两句自己的功夫。\n\n【主修功法】\n托塔天魔功，乱星海流传已久的顶级魔道功法，号称此地第一防御魔功。它的路数与正道相反：不养气，先炼体。真元不走经脉走筋骨，一层层压进皮肉里，压到最后一息之间周身如浇铁汁。\n练此功有三门：一曰扛，二曰忍，三曰反。扛得住同阶全力一击，忍得住一击之后的空档，反——把挨下的那一分力还回去。\n\n【能力自述】\n体修。同境界修士真元比我厚，但他们的法光打在我身上要打折。我这一身不是无敌，是耐打。\n皇鳞甲：随身多年的一件护身宝甲，已经送出去了。给谁了不重说，反正他拿它救过自己的命，比在我手里强。\n\n【几句实话】\n我进虚天殿两件事：采寿元果，抢补天丹。果没采着，丹拿到了——然后发现对我这个寿元将尽的人几乎没用。\n这就是体修最大的坑：壳再硬，里面那盏油还是会干。所以后来我看谁都说一句：别只顾着炼能挨打的身子，得想清楚你要拿这副身子去换什么。',
'乱星海第一防御魔功的三门：扛、忍、反。以及一个体修在寿元将尽时才承认的事：壳再硬，里面的油还是会干。', 111, '功法', '托塔天魔功,乱星海,体修', 1980, 3, 152, 68, 0, 1),

(1012, '象甲功与血炼神光：一具炼尸的战力上限',
'我叫曲魂。本名张铁，七玄门人，三属性缺木灵根——当年门里说我这辈子修不成长春功。\n\n【我练过的两样东西】\n一、象甲功。墨大夫所传，纯炼体，把皮肉练到刀枪不入。凡人练它，能打赢十个凡人；修士练它，只能多挨修士一下。\n二、血炼神光。这是后来才有的，走的是以血养气一路，出手带煞，同阶不敢硬接。代价是它每用一次，我这具身子就离「人」远一步。\n\n【我现在算什么】\n不人不尸。肉身是炼的，魂魄早没了，如今是一具能自己走路、自己打架的身外化身。能想事，但想起来隔层膜——高兴这种事，我记得定义，记不起感觉。\n\n【一句忠告】\n别学我。炼体这条路，练到尽头就是把自己变成一件法器。法器再利，也换不回一副能尝出肉干咸淡的身子。\n当年在七玄门，有人连自己那份都舍不得多吃。我如今什么都不必吃，倒常想起那一口。',
'一个没有灵根的凡人改修象甲功，最终把自己练成刀枪不入的炼尸：战力上限的代价，是把自己练成一件法器。', 112, '神通', '象甲功,血炼神光,炼尸', 1460, 3, 118, 44, 0, 1),

(1013, '极阴真解：摄魂、擒婴，与一具被打爆的肉身',
'贫道极阴祖师，乱星海修玄阴一脉。此文不辩正邪，只讲功法。\n\n【主修功法】\n极阴真解。至阴一路，练的是神识与寒气。三法：\n一、摄魂。以神念入他人识海，轻则乱其记忆，重则代其做主。修此法最忌贪，多探一寸，自己的念头就多染一分脏。\n二、擒婴。元婴出窍擒拿他人元婴，是此解里最狠也最不体面的一手——它杀不了人，但能把人变成一具还会走路的空壳。\n三、玄阴护体。寒气结膜，法术近身先自衰三分。\n\n【能力自述】\n我这一脉擅长的事只有一样：在你看不见的地方动手。斗法时我不喜硬拼，宁愿多等三日。\n异种火蟒：为了一桩图谋养了多年，最终便宜了别人。\n\n【认账】\n虚天殿那一趟，我算漏了一个人。后来我打爆过一个老怪的肉身、擒下他的元婴，多年之后那老怪被人放出来，当着人家的面把我吞了。\n一饮一啄，我没什么可喊冤的。修擒婴之术的人，早晚要明白一件事：你能锁住别人的元婴，锁不住自己的因果。',
'至阴一脉的摄魂、擒婴、玄阴护体三法，和一个修擒婴之术的人最终要明白的事：锁得住别人元婴，锁不住自己因果。', 113, '功法', '极阴真解,摄魂,擒婴', 1620, 2, 97, 39, 0, 1),

(1014, '六道轮回诀：魔道功法的三条铁律',
'六道极圣。魔道六宗出身，此名如何来的，不重要。\n\n【主修功法】\n六道轮回诀。它不是一门功法，是六门，按天、人、修罗、畜生、饿鬼、地狱六道分，每一道养一种性气。修到深处，一身之中六气并立，攻守、进退、生死各有其主。\n难在何处？难在六气不能有一气独大。哪一气压过别家，那人就成那一道。有人修到最后只剩修罗，见人就战，三百年内必死。\n\n【能力自述】\n功法、法宝、神通，我这一脉从不嫌多。人界灵气薄，能抢的都要抢——这不是贪，是明白一个道理：境界越高，越不是靠天赋，是靠手里还有多少底牌。\n\n【三条铁律】\n一、不许人欺我，也不许我轻敌。多少天才死在「他不过如此」四个字上。\n二、能用一件法宝解决的，不必动第二件。留着的东西才是自己的。\n三、想收弟子之前先看清：你要的是传人，还是容器。这两件事魔道里做得最多，也做得最难看。',
'六道轮回诀以六道养六气，难在一气不得独大；一个魔道巨枭的三条铁律，最后一条是：分清你要的是传人还是容器。', 114, '功法', '六道轮回诀,魔道,六气', 2350, 2, 176, 92, 0, 1),

(1015, '补天丹与虚天鼎：我那一夜的判断错在哪',
'万天明。正道万氏子弟，鉴宝三十年，只写一次看走眼。\n\n【我的本事】\n不靠功法靠眼力。万氏家学《照灵诀》：以神识贴着一物，看它灵气走向是否匀整。真器灵气内敛如一，伪器外润内乱。凭这一手，我在乱星海挣下了一份家业，也挣来一个「信得过」的名声。\n\n【那一夜】\n虚天殿开，鼎在前，丹在后。我判断：鼎大、人众、必乱，先取丹为稳。于是我拿到了补天丹。\n错在哪里？我算对了物，没算人。我只算了在场谁能打，没算谁愿意把命押上去。有个人从第一刻就没打算活着出去——这种人算不出来。\n\n【三条鉴宝心得】\n一、看物先看其主。一件无主之宝落在谁手里，比宝本身更值得看。\n二、越多人抢的东西越要慢一步。抢到手的人往往还要还。\n三、别把家学当自信。我的眼力三十年没错，错的那一次恰恰是在最不该错的地方。\n\n如今家中长辈仍说我那一夜不算败。我自己知道。',
'一个靠《照灵诀》鉴宝三十年的正道子弟，复盘自己唯一一次看走眼：算对了物，没算人。', 115, '见闻', '照灵诀,虚天殿,补天丹', 1290, 2, 86, 33, 0, 1),

(1016, '星宫律令与镇海大阵：秩序比神通难守',
'金魁，星宫大长老，元婴中期。今日不谈法，谈规矩。\n\n【星宫靠什么立足】\n不是靠高手，靠两样：航道与潮汛。哪片海有兽潮、哪条路能走货，星宫比谁都清楚。散修骂我们收过路钱，可没有这张图，他们连该往哪边退都不知道。\n\n【我修的功夫】\n星宫镇府功。这门功不出奇，长处是厚、是稳，与人对掌不吃亏，独缺一锤定音的手段。所以我的实权不在斗法，在阵。\n镇海大阵：三十六岛联动，阵眼常年由大长老守着。我守了百余年。阵这东西，十年不出事就没人记得它有用；出事那一年，才知道它值多少。\n\n【执法三条】\n一、律令要写在纸上，不能写在心里。写下来才能照着办，凭人心就会看人下刀。\n二、罚要早罚、小罚。拖到大事再办，就只能杀人了。\n三、别跟修士讲情面，讲例。今日破一次例，明日就有十个人来试。\n\n【一句自认】\n我这辈子办过不少叫人不服的案。但我敢把每一件都写进档里——这就是星宫还能撑着的理由。',
'星宫大长老自述：镇府功不擅杀伐但胜在厚稳，而真正让他立足的是一百多年没出事的镇海大阵与三条执法规矩。', 116, '宗门', '星宫,镇海大阵,律令', 1180, 1, 71, 29, 0, 1),

(1017, '元磁神光：与夫人同修三百年',
'凌啸风。天星双圣之一。外界传我们夫妇许多闲话，这里只讲功法。\n\n【元磁神光为何是两人修】\n此光分两极，一人身上养不齐。我与夫人各修其一，合之则成。单修者只能吸人法器，双修同使时，方圆百丈之内一切金铁之物皆听调不听宣。\n元婴后期二人联手可短暂抗住化神，靠的就是这个：不与你斗法，先收你兵。\n\n【三百年里最难的不是行气】\n是「同频」。两门功法一阴一阳，须同时起、同时收。有一年我急于突破，快了一线，夫人的气机被带乱，反噬了半年。从那以后我们定下一条：谁状态不好，两人一起停下。\n双修之术被世人想歪了，它真正难的地方在于：你要把另一个人的呼吸当成自己的进度表。\n\n【能力自述】\n元磁神光、星宫镇海阵的半边阵眼、一副用惯的旧甲。\n\n【一句话】\n这门功夫最大的破绽不是被隔开两人，是有一人心不诚。气可以骗人，心骗不了。',
'天星双圣的元磁神光为何必须两人同修：不与你斗法、先收你兵，以及三百年里最难的不是行气而是同频。', 117, '双修', '元磁神光,天星双圣,同频', 1740, 2, 134, 61, 0, 1),

(1018, '化神期的风景：人界顶点这三千年',
'向之礼。化神初期，人界如今能说出口的几个人之一。\n\n【化神到底是什么】\n元婴以下，你在用灵气；化神，你能叫得动天地。不是量变，是换了一种活法：真元不必从自己身上取，一呼一吸山河皆为你用。\n所以化神打元婴，从不斗久。天地之气站在谁那边，这一战就完了。\n\n【人界最残酷的地方】\n灵气薄。薄到化神在这里是「多余」的——你有叫动天地的本事，可这天地的灵气不够你施展太久。\n我三千年没等到飞升。不是不够强，是这道门在人界已经关了。\n\n【我这三千年在做什么】\n一、守着。人界若来外敌，化神不出，就没有能站出来的。\n二、记着。上古如何、灵气如何、飞升如何，一代代记下来，免得后人从头猜。\n三、教着。见过一个年轻人，四属性伪灵根，不声不响。我当时没点他——这种人不需要人点，点了反倒害他。\n\n【一句结语】\n境界不是风景，是责任的大小。你现在觉得日子难，是因为难的事还有人能管。等我这样的人管不动了，才知道什么叫真难。',
'人界化神修士的三千年：化神不是量变而是换一种活法，而最残酷的是人界灵气已薄到化神多余、飞升无门。', 118, '见闻', '化神,人界,飞升', 2510, 2, 198, 107, 0, 1),

(1019, '云露魔光：一个魔道修士的女儿',
'云露老魔。魔道里活了很久，久到不想再证明自己活得久。\n\n【主修功法】\n云露魔光。取「雾里藏刀」之意：放出的魔光不伤人，先迷神。人在自己最想要的幻象里待上三息，三息之后身上有什么，就不是自己的了。\n此法有个毛病：用多了，自己也分不清哪一重是真的。我如今常常在夜里坐起，摸一摸手腕，确认还在。\n\n【一个魔道修士的家务事】\n我有个女儿。她娘不肯让她认我，这我认。魔道的爹，正道不认，她娘要护她，只能这么办。\n我修了一辈子迷神之法，唯一没迷过的一次，是站在她宗门外看了半日，没进去。\n\n【给后辈的话】\n魔道路快，是因为它把代价往后推。推到什么时候、推给谁，你事先从来不知道。\n我现在只想知道一件事：她如今剑练得如何。这话不打算对谁说，写在这里，也是白写。',
'云露魔光先迷神后取物，用多了自己也分不清哪一重是真；一个魔道老怪唯一没迷过的一次，是站在女儿宗门外看了半日没进去。', 119, '功法', '云露魔光,魔道,迷神', 1420, 2, 103, 47, 0, 1),

(1020, '凡人替仙师看着一具炼尸：四平帮的账本',
'孙二狗，四平帮帮主。不会法术，斗大的字认得一百多个。\n\n【我的活】\n仙师走之前留了一句话：「看着它。」\n它是一具炼尸，刀枪不入，力大无穷，白天站在院里不动，晚上会自己走到门口站着。\n我看了十几年。头两年吓得睡不着，后来发现它比帮里多数人都守规矩——不吃饭、不赌钱、不偷人东西，叫它站着就站着。\n\n【怎么和它相处】\n我试过跟他说话。起初不应，后来有一回我说今日下雨，他抬了一下头。\n我不敢多想。仙师回来问一句「它安分么」，我说安分。这话说得我心虚，因为我觉得他安分得让人心疼。\n\n【凡人管修士的东西，三条】\n一、别拿他的东西。院里那口棺谁都不许碰，帮里有人手痒，我亲自打断了他的手指。\n二、别问。他为什么站这儿、等谁，问了我也答不上，还容易惹祸。\n三、账要记清。仙师赏的灵石我一枚没动，收支都记在本子上，什么时候他都可以在这里查到。\n\n【最后一句】\n我这一生大概就这样了。但仙师教我那兄弟说过一句：能活着走到最后的，都不是最能打的。我把这句话也记在本子第一页。',
'一个不会法术的凡人帮主，替仙师看了十几年炼尸：不拿、不问、账记清，以及那具炼尸让他心疼的地方。', 120, '见闻', '四平帮,炼尸,凡俗', 1080, 2, 92, 26, 0, 1);

-- 女修十篇（1021-1030）
INSERT IGNORE INTO blog_article (id, title, content, summary, author_id, category, tags, view_count, comment_count, like_count, favorite_count, is_top, status) VALUES
(1021, '翠云诀：宗门里的女修怎么活',
'陈巧倩，黄枫谷弟子，筑基初期。这篇不谈风月，只谈活法。\n\n【主修功法】\n翠云诀。木属性功，性子慢，长处是养人：受了伤比别人好得快，灵力不烈，胜在能耗。\n短处也说清楚：它出不了重手。同阶斗法，我多半是那个拖到对方没耐性的。\n\n【宗门里女修真正难的地方】\n不是功法、不是资源，是「便宜」。\n师兄弟顺手帮你一次，日后这就是一笔账；长老多看你一眼，同门就要多想你三句。你做得好，人说你有背景；你做得不好，人说果然如此。\n\n【三条我自己在用的规矩】\n一、人情当场还清。灵石、丹药、人情，能当日还的当日还，欠着的东西早晚要拿别的补。\n二、不接单独的好意。若真要谢，请当着同门的面说，话说在亮处，账就留在亮处。\n三、被人误会时不哭，去查档。哭一次，旁人便觉得你理亏；把出入、丹药、值夜的日子摆出来，一次就够。\n\n【一句结语】\n我后来见过一个替我挡了一下的同门。他做得极平常，平常到根本没打算告诉我。\n宗门里不全是那样的人。这一句我留着，不为别的，免得自己把所有人都看扁了。',
'翠云诀性子慢、养人但出不了重手；一个黄枫谷女修在宗门里立身的三条规矩：人情当场还清，不接单独的好意。', 121, '功法', '翠云诀,黄枫谷,女修', 1360, 2, 104, 45, 0, 1),

(1022, '我没有灵根：凡俗女子如何与修士相处',
'墨彩环。家中排行第三，没有灵根——这句话我十四岁那年就学会自己先说了，免得别人替我说。\n\n【我家那一摊】\n爹爹行医，也修长春功。他教我认药、记账、看人脸色，唯独没教我修行。\n我后来才明白：不是他小气。没有灵根硬引气，轻则伤身，重则瘫在床上。他宁可我骂他。\n\n【凡俗女子和修士之间】\n我年少时想过一件事：若我能共修大道，是不是就能走得很远。\n如今想清楚了。寿命差在那里，我陪不了他几十年，他连送我都不用费神——我这样的凡俗，他见着一茬又一茬。\n把心思放在「叫他也看我一眼」上，是把自己活成了一件事。我不干。\n\n【我这三十年】\n一、把医道学下来。爹爹的手艺如今是我的，凡俗人的病我治，修士的伤我也见过。\n二、把家撑住。门里门外的事有人总得管，修士不管这些，他们觉得这些不算是本事。\n三、不怨。我这一辈子没什么灵缘，可我也没饿过、没被人欺过、还救过人。这不算惨。\n\n【一句实话】\n若真有下辈子，我想有个灵根。这话我娘生前不许我说，如今我自己说说，不犯法。',
'墨家三女自述：没有灵根不是不能修，是硬修要瘫在床上；把心思放在叫谁看自己一眼上，是把自己活成了一件事。', 122, '见闻', '墨家,凡俗,长春功', 1240, 2, 98, 37, 0, 1),

(1023, '血色试炼求生录：灵兽山的御兽心得',
'菡云芝。灵兽山这一脉修的是御兽，不是驭兽。这两个字差一个字，差一辈子。\n\n【主修功法】\n《百灵谐鸣诀》。以神识与兽念相接，不压、不锁，只讲「合」。\n驭兽是用禁制把兽压服，兽一强就反噬；御兽是让它觉得跟你一起划算。前者养出工具，后者养出伙伴。\n\n【血色试炼里我活下来的原因】\n那年太南庙会的旧识都进了试炼。里头三件事救了我：\n一、我从不与兽硬拼，先绕。我的兽帮我找路，不帮我拼命。\n二、天黑前一定停下。夜里那片林子的东西不是给人留的。\n三、看见别人斗得两败俱伤，我绕远路走。当时觉得丢人，后来那两人一个死了一个残了。\n\n【能力自述】\n我最拿得出手的不是法术，是养兽：一只二阶灵禽跟了我一百多年，如今通人性，能替我看方向。\n结丹我成了。慢，但成了。\n\n【一句话】\n御兽这门功夫教给我的，不是怎么叫兽听话，是怎么叫一个比你活得久的东西愿意陪你。',
'百灵谐鸣诀讲合不讲压：驭兽养出工具，御兽养出伙伴；血色试炼里救她一命的三件事，全都不是硬拼。', 123, '神通', '御兽,百灵谐鸣诀,血色试炼', 1150, 1, 82, 31, 0, 1),

(1024, '名义道侣：一场交易里的分寸',
'慕沛灵。修行多年，功法平平，今日只写一件我做得还算漂亮的事。\n\n【我的功法】\n《静明诀》。家传的一门静功，不求杀伐，专养神识清明。长处是：我答应过的事、欠下的账、说过的话，我记得极清楚。\n这门功到后面进境很慢，我卡在这里许多年，寿元也在走。我不怨。\n\n【名义道侣是什么】\n一纸约定，各取所需。他有他要挡的事，我有我要靠的地方。\n这种关系最难的不是外人说什么，是自己心里那杆秤：什么时候该退一步，什么时候一步都不能退。\n\n【我守的三条】\n一、不讨不该讨的。名分是纸，情分不是。拿纸去要情，两边都难看。\n二、不坏别人的事。他家里那位从没见过我，也不该见。这不是我大度，是我知道那东西一旦被摆到桌上，谁都不体面。\n三、把自己活住。我修不成大道，就把家族里那几个孩子教出来。人不能只靠一段关系活着。\n\n【结语】\n我这一生大约就这样了。但我不欠谁的。这一句我说得出声，睡得着觉。',
'静明诀养的是神识清明：名义道侣最难的不是外人说什么，是自己心里那杆秤——什么时候退一步，什么时候一步不退。', 124, '双修', '静明诀,名义道侣,分寸', 1330, 1, 95, 40, 0, 1),

(1025, '大晋坊市的丹药铺：我见过的三种假丹',
'曹梦容。在大晋行走多年，如今开一间小铺子，只卖丹药，不问来历。\n\n【我的本事】\n不靠功法靠鼻子。《辨药要录》里有一条：真丹香气沉，半日不散；假丹香浮，一开瓶就冲。\n我在坊市这些年，靠这一手躲过三次祸，也送走过两个熟人。\n\n【三种假丹，一种比一种要命】\n一、以次充好。筑基丹当凝丹丹卖。吃不死人，但把你十年的灵石和一年的机会一起吞了。\n二、药性偏激。用猛药催出丹相，看着成色极好。服下当时灵力大涨，半年之后经脉自己就脆了。这种最多，因为最容易骗到急著突破的人。\n三、借名。拿大宗的名号、拿故人的方子，卖的是自己乱配的。这种最毒，因为它骗的不只是一炉丹，是一整个坊市的信。\n\n【我铺子的规矩】\n一、来路不明的不卖。\n二、自己不认得的方子不炼。\n三、卖错了认赔，当日赔，不拖。\n\n【一句实话】\n修士买丹，多半不是买药，是买一个「我还能再进一步」的念头。\n我卖了这个念头半辈子，只有一条底线：不卖假的。',
'大晋坊市丹药铺自述：三种假丹一种比一种要命，而修士买丹买的其实是一个还能再进一步的念头。', 125, '丹药', '辨药,假丹,大晋', 1210, 2, 88, 35, 0, 1),

(1026, '玄岩龟甲：防御型妖兽的千年修行',
'圭灵。昆吾山十级妖兽，本体玄岩龟。说话慢，字少，你们多担待。\n\n【我这一身】\n壳。八百年长成的壳，能挡同阶全力一击。\n除了壳，我没本事。不会飞，不擅追，神通只有一个：扛。\n\n【龟的修行法】\n人修功法讲精进，我们讲省。\n一、能不动就不动。灵气在壳里自己转，动一次要养三年。\n二、能不吃就不吃。山上一株灵草，我闻着香，可以三年不碰，等它熟透自己掉下来。\n三、能不见就不见。有人上山，我缩进壳里。他们骂我怯，我活到了今日。\n\n【寿元的看法】\n你们修士最怕寿元不够，我最怕的是看人换了一茬又一茬。\n我守着昆吾山，看着上山的人从少年看到白头。有一回一个元婴小辈问我：龟前辈，你不怕么？\n我说：我怕的东西你们都不怕，所以你们才活得那么快。\n\n【一句】\n防御不是怯。是知道自己要守什么。我守山八百年，如今换个地方守。守的东西变了，壳没变。',
'玄岩龟的修行法只有三个字：省、慢、扛。能不动就不动，能不见就不见——防御不是怯，是知道自己要守什么。', 126, '神通', '玄岩龟,妖兽,守山', 1090, 1, 76, 28, 0, 1),

(1027, '九尾一脉的重建：从灵寰界开始',
'柳乐儿。我认识韩前辈时还是个孩子。如今族里的事归我管。\n\n【我们这一族】\n九尾一脉，靠血脉吃饭，也败在血脉上。\n修行《凝月诀》：取月华入体，一层一层长尾。一尾一境界，九尾圆满可敌化神。\n难在两处：一是尾越长，天劫越重；二是族中子弟一旦少了，血脉就薄，薄到后面连开灵智都难。\n\n【我接手时的情形】\n没几个孩子，老的全是伤。我做的第一件事不是教功法，是定规矩：\n一、不许吃人。这一条写在祠堂门口。族里有人嫌我死板，我不管。吃了人，就再也回不了人形，也进不了任何一座城。\n二、不许内斗。血脉已经薄了，再自己消耗，一族就完了。\n三、谁有灵智谁教孩子。修为高低不管，教孩子要的是耐心。\n\n【能力自述】\n我如今凝了几尾，自己不说。我真正的本事不是法术，是把一群小东西养大。\n\n【一句】\n银月前辈当年在剑里教过我一句话：活得久不算本事，活得像自己才算。我拿这句话做了族训。',
'九尾一脉靠《凝月诀》取月华长尾，一尾一境界；接手时她做的第一件事不是教功法，是在祠堂门口写下一条不许吃人。', 127, '宗门', '九尾,凝月诀,灵寰界', 1280, 2, 91, 36, 0, 1),

(1028, '白凤峰剑诀：虚名与实剑',
'白凤。落云宗白凤峰峰主，元婴初期。宗门里给我加了个「第一美女」的名号，我摘不掉，只能把它和剑分开说。\n\n【主修功法】\n《白凤三剑》。就三式，练了一百多年。\n一曰照：剑光亮处，对手身形先滞一分。\n二曰断：一气斩下，不取巧，专破护身灵光。\n三曰归：收剑即止。这一式最难，因为它要求你在能杀的时候停。\n\n【为什么只练三式】\n我年轻时也贪多，学过十七种剑招，结果卡在金丹三十年。\n后来把十七种全丢了，只留三式，反复打磨。第五十年上，我忽然明白：剑不是用来打赢的，是用来叫你不必多说话的。\n\n【关于那个名号】\n它对我有害无益。人先看了脸，再听你说事，你说得再对，他们也觉得你靠的是脸。\n我的办法是不解释，只办事。宗门里议阵、议矿、议弟子分配，我一次没缺席。二十年后，再没人拿这句话当理由。\n\n【一句】\n三式剑，一式收。练到能停手的时候，才算真的会了。',
'白凤三剑只有三式，最难的是第三式归——要求你在能杀的时候停；至于那个虚名，她的办法是不解释，只办事。', 128, '功法', '白凤三剑,落云宗,元婴', 1400, 1, 109, 48, 0, 1),

(1029, '正道第一人的日常：约束比神通难',
'万三姑。乱星海正道这一边，如今话最重的人是我。这不是夸口，是负担。\n\n【我的功法】\n《太素元真诀》。正道绵长一路，真元厚而不烈，最善守、最不善杀。\n我这一生亲手杀人极少，少到能数清。不是不能，是不能随便。\n\n【位置越高越奇怪的地方】\n人人都有事来找你，而你能应的事越来越少。\n有人被杀了，来要我主持公道；我若出手，明日就有另一个人说我也该死。所以我必须先定一条：什么事算事。\n这条定得越清楚，骂我的人越多。我挨了百余年，习惯了。\n\n【虚天殿那一夜我为什么没去】\n我侄儿在里面。我要是去了，正道与魔道在那座殿里就会先打起来，死了的人更多，而且死的人里会有不该死的。\n我忍住了没去。事后他回来，叫我一声姑，什么也没说。\n\n【三条自守】\n一、能讲例的不讲情，能讲情的不讲力。\n二、晚辈之间的事，只断是非，不代他活。\n三、自己门下犯事，当日办，办得比旁人重。\n\n【结语】\n神通是你能打过多强的人。正道是你能对自己人下多重的手。这两件事，后者难得多。',
'太素元真诀厚而不烈、最善守不善杀；神通是你能打过多强的人，正道是你能对自己人下多重的手。', 129, '宗门', '太素元真诀,正道,乱星海', 1520, 1, 116, 52, 0, 1),

(1030, '红拂三剑：当年我如何从魔道手里带走女儿',
'红拂。今日只写剑，不写旧人。\n\n【主修功法】\n《红拂三剑》。一剑斩禁制，二剑断神识相接，三剑不为胜、为走。\n第三式是我自己改的。年轻时我把它放最后，如今我把它放最前——能走，比能胜值钱。\n\n【那一夜】\n我要带走一个刚出生的孩子。\n魔道那一边的规矩我清楚：他们不拦强者，只拦心软的人。\n所以我那夜做了三件事：\n一、先毁后路。把追兵要用的传送阵一剑劈了，也断了自己的退路——人只有没退路时才不会犹豫。\n二、不战而破。我不与他们对掌，只斩他们手里的禁制旗。旗碎，几人便先自顾。\n三、不回头。孩子哭，我走我的。回头一次，就走不了。\n\n【这些年】\n她如今很好，好到不愿认我。\n我不怨。她娘把她养成了不怕魔道的人，这就是我那夜想要的东西。\n\n【一句】\n当母亲的剑不为取胜，只为叫一个人能抱着孩子走出去。走出去之后，这把剑就可以收起来了。',
'红拂三剑：一剑斩禁制，二剑断神识，三剑不为胜为走；那一夜她先毁了自己的退路，因为人只有没退路时才不会犹豫。', 130, '法宝', '红拂三剑,剑修,母女', 1450, 1, 121, 55, 0, 1);

-- 第二批互评：条数与上面每篇的 comment_count 严格对齐（合计 35 条），
-- 评论沿着人物关系写，不写成泛泛夸奖——蛮胡子↔极阴祖师、曲魂↔墨大夫↔孙二狗、
-- 云露老魔↔红拂↔董萱儿、万三姑↔万天明、凌啸风↔金魁↔冰凤、柳乐儿引银月族训。
INSERT IGNORE INTO blog_comment (id, article_id, user_id, parent_id, content, status) VALUES
(1018, 1011, 113, 0, '老蛮，你把壳练得再硬，里面那点油还是那么多。这话你肯写出来，我便不与你摆正邪的架子。', 1),
(1019, 1011, 101, 0, '那件甲在我身上穿了百年，救过两次命。前辈若还在世上，这杯酒我隔空敬你。', 1),
(1020, 1011, 112, 0, '炼体的尽头是把自己炼成一件器物。前辈至少还想清楚要拿它换什么，我没想清楚就上路了。', 1),
(1021, 1012, 103, 0, '象甲功是我传的，也是我叫你去撞那面墙的。这句话我没脸再说第二遍，今日写在纸上。', 1),
(1022, 1012, 120, 0, '你白天在院里站着的时候，有时一片叶子落在你肩上，我不敢拂。你抬头那一下，我在廊下看见了。', 1),
(1023, 1012, 101, 0, '张铁。这三个字我不常敢说。你如今还肯自己发帖写事，比我那几年强。', 1),
(1024, 1013, 111, 0, '你擒过我的元婴，我也差点拆了你的鼎。如今都坐在这儿写心得，可见谁都没死透。', 1),
(1025, 1013, 107, 0, '摄魂贪多，识海必脏。这一条你肯落在纸上，比你那三法值钱。', 1),
(1026, 1014, 113, 0, '魔道里能把「传人还是容器」说出口的，你是头一个。我修擒婴一辈子，到被人吞了才懂。', 1),
(1027, 1014, 129, 0, '六气不许一气独大，写得好。只是你写这一篇的时候，开口的是哪一道？', 1),
(1028, 1015, 116, 0, '那一夜星宫的人在殿外守着。你说「算对了物，没算人」——我这辈子写律令，算不出愿意把命押上去的那种人。', 1),
(1029, 1015, 109, 0, '乱星海只认两样：背后的势力、手上的货。你三十年眼力抵得上一支船队，所以那一次才被所有人记住。', 1),
(1030, 1016, 117, 0, '镇海大阵那半边阵眼是我和夫人轮值。你守了一百年，我们守了三百年，谁都没对外人说过这阵有用——今日你说了。', 1),
(1031, 1017, 110, 0, '「把另一个人的呼吸当成自己的进度表」。化神那道坎上我读到这一句，坐了一整夜没动。', 1),
(1032, 1017, 116, 0, '三百年不停一次，比破一次阵难。这话只有守过阵眼的人才说得懂。', 1),
(1033, 1018, 110, 0, '「化神多余」这四个字，我等了三百多年才有人写出来。原来雪地里坐着的不是只有我一个。', 1),
(1034, 1018, 101, 0, '当年你说没点我。后来我自己走的每一步，都在替你那句话作证——点了反倒害他。', 1),
(1035, 1019, 130, 0, '你看了半日没进去。我在门里也站了半日，隔着窗。这两个半日加起来，算我们见过一回。', 1),
(1036, 1019, 105, 0, '前辈叫我一声弟子就好。我修的是木属性慢功，那点血统在我身上一点用都没有——这大概是我听到的最好的消息。', 1),
(1037, 1020, 104, 0, '兄弟。八极拳的起手式我还记得。你那本子上的字比我多，我在门里只认得自己的名字。', 1),
(1038, 1020, 112, 0, '那日你说今日下雨。我抬头不是听见了，是肩上落了雨。但我没告诉你，怕你失望。', 1),
(1039, 1021, 105, 0, '药园里我也守「当日还」这一条，只是没人教我「话要说在亮处」，那是我吃了三年亏自己想出来的。', 1),
(1040, 1021, 102, 0, '掩月宗也有这三条，只不过我们管它叫戒律。你能自己悟出来，比我守得牢。', 1),
(1041, 1022, 103, 0, '爹爹不教你引气，不是小气，是他没本事给你挣来合适的灵物。这一句我还你，迟了三十年。', 1),
(1042, 1022, 104, 0, '治凡俗人的病也是修行。这些年七玄门被你医回来的人，比你爹记得清楚。', 1),
(1043, 1023, 101, 0, '「怎么叫一个比你活得久的东西愿意陪你」——这句替我说尽了。我那位在剑里陪了我几百年，从没说过一个累字。', 1),
(1044, 1024, 108, 0, '不欠谁的，所以睡得着。我在剑里见过太多把自己抵押出去的人，有拿情分抵的，也有拿法宝抵的。', 1),
(1045, 1025, 105, 0, '第三种那条我撞见过：拿我们药园旧历年的名头卖外路货。药园的出入账我可以陪你对。', 1),
(1046, 1025, 122, 0, '猛药催丹相那种，凡俗身子吃下去直接瘫在床上。我在城里治过三个，开口都问我丹是哪儿买的。你把规矩立在这里，就是救人。', 1),
(1047, 1026, 127, 0, '龟前辈，小时候族里长辈带我去昆吾山下，你说「我怕的东西你们都不怕」。这句我记到今天，如今拿它教族里的孩子。', 1),
(1048, 1027, 108, 0, '我在剑里说过的话早记不全了。你能拿它做族训，那它就不是我说过的话，是你的了。', 1),
(1049, 1027, 101, 0, '「不许吃人」写在祠堂门口，比修满九尾难。你做到了这一条，就再没人敢把你当孩子。', 1),
(1050, 1028, 129, 0, '三式剑，一式收。我忍了百余年不出手，等的就是有人肯把「能杀时停」写成一句教人的话。', 1),
(1051, 1029, 115, 0, '姑。那一夜你没进殿，我活着出来了。这笔账我一直是这么算的，从来没改过。', 1),
(1052, 1030, 105, 0, '娘。我不认的是「魔道」那两个字，不是这把剑。这句话我攒了二十年，写在这儿，你不必回。', 1);

-- ==================== 人界篇第三批补发文（131-173）====================
-- 这一批只为补齐「主页有文章、有点赞有评论」，正文以 CultivationCatalog 设定为准，第一人称口吻。
-- 文章 1031-1073，每篇恰配 1 条互评（评论 1053-1095，与 comment_count 一一对齐，重跑幂等）。

-- 天南一线（1031-1038）
INSERT IGNORE INTO blog_article (id, title, content, summary, author_id, category, tags, view_count, comment_count, like_count, favorite_count, is_top, status) VALUES
(1031, '十绝毒：我把毒摆在你面前，你也要喝',
'老夫魏无涯，化意门太上长老，天南三大修士之首。名头是别人怕出来的，毒是我自己一口一口炼的。\n\n【主修功法】\n万毒混元功。修毒先修己：把全身泡成药，十绝毒、腹尸之毒、绿毒化蛇，一味比一味刁。练这门功最忌怕死——自己先怕了，毒就压不住反噬。\n\n【能力自述】\n我不擅正面斗法，擅的是叫你中了一手之后越打越虚。腹尸之毒不打身，打你吞下去那口气，等你觉出不对，已经晚了半息。破魂蛇养六十年只放一次，放出去的不是毒，是一条有名字的蛇。\n\n【一句实话】\n正道骂我用毒阴人，我认。可我这一辈子没在背后算计过谁——毒摆在你面前，你也要喝。这才是毒道该有的傲气。',
'化意门太上长老自述：修毒先修己，破魂蛇六十年只放一次；正道骂他用毒阴人，他只认一句——毒摆在你面前你也要喝。', 131, '功法', '万毒混元功,十绝毒,天南', 1720, 1, 132, 61, 0, 1),

(1032, '至阳真火：三人里我最弱，可我守的门最结实',
'贫道至阳上人，太真门太上长老，天南正道这一边的门面。三大修士里我修为最弱，这话我当众认。\n\n【主修功法】\n静心诀 + 至阳真火。真火至阳至刚，不烧人，专烧阴寒法器——魔道修士怕的不是我，是我这口火。静心诀练的是不上头：任你先乱我心，我自一寸不乱。\n\n【能力自述】\n我不擅斗久，所以起手先借太真门阵势；阵成了，才轮得到讲道理。焚天阵金光万道那一刻，天南没人敢进阵。\n\n【几句实话】\n当年天恨老怪把我打成过轻伤，我记着，也认——他比我敢下手。可我守的那道门、布的那座阵，是三人里最结实的。正道要的从来不是最能打的那个，是最后一个倒的。',
'天南正道第一人不讳言自己最弱：静心诀练不上头，至阳真火专烧法器，正道要的是最后一个倒下的那个。', 132, '功法', '静心诀,至阳真火,太真门', 1580, 1, 118, 54, 0, 1),

(1033, '合欢魔功：正道讲规矩，我们讲效果',
'老夫合欢老魔，合欢宗大长老，天南第一魔修。正邪这两个字我懒得辩，辩了也还是这碗饭。\n\n【主修功法】\n合欢魔功，阴姹女、阳婴儿一路。采补而不伤根——最难的不是吸，是吸完还要叫你的经脉看不出被吸过。一气化三是顶峰：三具化身同使一门功，你杀了我一次，剩下两次我还要跟你讲人情。\n\n【能力自述】\n天南骂我爱偷袭，骂对了。正面打不赢的人，我凭什么要正面打。我从不与人拼第一击，前几句都是客气，客气里已经把你算完了。\n\n【一句实话】\n我这一身没有一样本事是干净的，可它叫我活到今天——靠的就是不害臊。',
'天南第一魔修不谈正邪只谈效果：采补最难的是吸完还要叫人看不出，三具化身同使一门功，杀我一次还有两次。', 133, '功法', '合欢魔功,阴姹女,魔道', 1660, 1, 124, 58, 0, 1),

(1034, '天恨魔光：我这一恨记了三百年，今日原样还你',
'老夫天恨老怪，天南魔道散修。三大修士之下第一人——这名号不好听，但叫我活到了现在。\n\n【主修功法】\n天恨魔光。光不蚀肉，先蚀神：照见你一生最不甘的那件事，叫你愣上三息。三息足够我走，也够我出剑。我这门功的力气全在气头上——谁叫我冷静，谁就是在削我。\n\n【能力自述】\n我打伤过三个比我名头大的人，至阳上人是其中一个。此事天南知道的不多，所以我现在还活着。散修的老命是靠腿挣的，名头越大越该带两条好腿。\n\n【一句实话】\n我这一恨记了三百年。恨谁不重要，重要的是它一直在替我攒力气。今日我原样还给你。',
'三大修士之下第一人自陈：天恨魔光先蚀神后出剑，力气全在气头上；一恨记了三百年，恨谁不重要，它在替他攒力气。', 134, '功法', '天恨魔光,魔道散修,天南', 1420, 1, 96, 43, 0, 1),

(1035, '鸾鸣九响：领军不是最能打的，是最后退的那个',
'老夫龙晗，鸾鸣宗太上长老，天道盟领军。领军两个字的分量，是替别人扛出来的。\n\n【主修功法】\n鸾鸣九响。以音为号，联阵攻坚：一声起，二声断你指挥，三声你身后那座阵自己就乱了。我不独斗——天道盟的仗从来不是两个人打，我一声令下，七位元婴各安其位。\n\n【能力自述】\n为后队断声这一手不杀人，专把追兵的声音压住；领军是最后退的那个。凤龙合鸣是与道侣同使的一击，两人中期，打出一个后期的数。\n\n【一句实话】\n盟里都说我修为不算顶尖，我不辩。能叫一群素不相识的元婴肯听我号令、各自站位、谁也不退——这门功夫比一记神通难，也比它值钱。',
'天道盟领军自述：鸾鸣九响以音联阵，一声断指挥、三声阵自乱；领军不是最能打的，是最后退、替众人扛的那个。', 135, '神通', '鸾鸣九响,天道盟,音攻', 1360, 1, 88, 40, 0, 1),

(1036, '寒凰真气：他出头，我算账',
'妾身凤冰，鸾鸣宗人，龙晗的道侣。盟里的事他出头，海上的账我算——两件事本来就得有人分开做。\n\n【主修功法】\n寒凰真气。此寒不冻人身，先冻灵气之间那道牵连：你法诀里那一处转弯被我冻住，转不过来，一击就散了。\n\n【能力自述】\n我最拿得出手的是牵制。当年独力拖住大晋慕兰一位神师，靠的不是硬扛，是不跟他打。冰封一角阵，不冻全场只冻一角——角里那位今日就别想支援任何人。\n\n【一句实话】\n人前他说话，我不动声色。可盟里真到算总账的时候，那笔账是我平的。夫妻两个中期硬凑出一个后期来，这才是鸾鸣宗叫人忌惮的地方。',
'鸾鸣宗凤冰自述：寒凰真气先冻灵气那道牵连，独力牵制大晋神师靠的是不跟他打；盟里他出头，海上的账她来算。', 136, '功法', '寒凰真气,鸾鸣宗,牵制', 1310, 1, 84, 37, 0, 1),

(1037, '化剑为丝：剑修到老，比的是敢不敢把自己也炼成剑',
'老夫金武环，古剑门大长老。一辈子只做一件事：把一剑分成万千丝。\n\n【主修功法】\n化剑为丝。一剑散成三千丝，丝不伤人，先把人困住——困住了才谈剑。丝阵一成，收口只在一息，这一息里你的护身灵光跟没有一样。\n\n【能力自述】\n我把本命剑也炼成丝绕在身上。守的时候，我自己就是剑。丝尽剑亡是最后一步：三千丝并成一线放出去，我三百年道行就只剩一半。当年与一位后进切磋，我败在他那对风雷翅和一口乾蓝冰焰上，此事我认账。\n\n【一句实话】\n剑修到了我这把年纪，比的不是快，是敢不敢连自己也炼进剑里。别人留一手保命——我这一身，早就是那一手。',
'古剑门大长老自陈化剑为丝：一剑散三千丝先困后收，把本命剑也炼成丝绕身；剑修到老比的是敢不敢把自己也炼成剑。', 137, '功法', '化剑为丝,古剑门,剑修', 1490, 1, 105, 47, 0, 1),

(1038, '凝光镜：掩月宗的剑，不拿来讲亲缘',
'在下南宫阙，掩月宗元婴剑修，使一面凝光镜。同为南宫，与婉妹不熟——宗门里的剑，不是拿来攀亲的。\n\n【主修功法】\n凝光剑诀。以镜收光，光尽剑至：镜不伤人，只把你那一剑的光收进去，下一剑还给你——用的是你的光、我的剑。\n\n【能力自述】\n掩月一剑，月缺时出，这一式不取名，因为它只要一息。镜里存着三段别人的旧剑光，挨打时放一段出去顶。镜光齐放是压箱底一手：这镜子攒了我半生的对手，今日一并还给你们。\n\n【一句实话】\n有人拿我和婉妹比。我说稀奇——她修她的素女元阴，我磨我的凝光镜，一条道上没打过照面。剑这个东西，谁的账谁自己还。',
'掩月宗元中剑修自述：凝光镜收你一剑之光、下一剑还你，攒了半生对手今日一并还；同为南宫却与婉妹不熟，剑不拿来攀亲。', 138, '法宝', '凝光镜,掩月宗,剑修', 1380, 1, 92, 41, 0, 1),

(1039, '黄枫谷唯一的元婴：我守的是黄枫谷',
'老夫令狐，黄枫谷人，谷里弟子唤我老祖。这点修为摆出去，天南没人当回事——可我是黄枫谷唯一的元婴。\n\n【主修功法】\n黄枫谷旧法，不擅杀伐，擅守谷。我这一辈子出手，多数在谷口，不在别处。\n\n【能力自述】\n老怪这张脸不值钱，但能把一场仗推到明日再打。谷中弟子都躲在我身后，我这一口真气就不能散。一门之气尽付我手：一座小峰三百年不出名，靠的就是有人肯站在门口。\n\n【一句实话】\n当年谷里出了个走运的小辈，后来成了大事。我没教过他几手，他也没提过我。这不碍事——他站在人前的时候，我在谷口替他把后路看着，就够。',
'黄枫谷唯一的元婴自述：不擅杀伐擅守谷，一门之气尽付我手；当年那个走运的小辈成了大事，他在人前，他在谷口看着后路。', 139, '宗门', '黄枫谷,守谷,元婴', 1290, 1, 79, 35, 0, 1),

(1040, '万尺一线：退路就是我的宗门',
'在下南陇侯，天南散修。散修没有宗门——退路就是我的宗门。\n\n【主修功法】\n万尺一线，苍坤上人独创的遁术。不是一步千里，是把自己压成一条线，线比你先到位。\n\n【能力自述】\n我不与人换手，攒了三十招才舍得出一次，这叫存了一辈子的一击。见势不对先退——这道护身不是为了挡住你，是为了叫我多活那半息去起遁。先活下来再谈道，这是苍坤创这一式时想明白的事。\n\n【一句实话】\n天南笑话我只会跑。跑有什么不好？跑得掉的仗才轮得到你明天接着打。我这一身没别的长处，就是到现在还站着，而当年比我横的那些，坟头草都三丈了。',
'天南散修自述万尺一线：把自己压成一条线、线比人先到位；退路就是宗门，跑得掉的仗才轮得到明天接着打。', 140, '神通', '万尺一线,遁术,天南散修', 1340, 1, 86, 38, 0, 1),

(1041, '碎魂大法：人身好护，魂难护',
'在下碎魂真人，天南散修。这一脉不击身，直取魂。\n\n【主修功法】\n碎魂大法：先碎你魂魄，再谈胜负。碎魂一指不伤皮肉，直进识海——你身上有多少护身法宝都没用。\n\n【能力自述】\n乱神摄魂，先叫你对不上法诀里那一口气，后打。修碎魂的人自己最怕被碎，我这手护魂守一，比功法练得久。你元婴出窍那一刻，就是我最快的一刻。\n\n【一句实话】\n人身好护，魂难护。这一句我写在帖子上，不是写在心里。因为写进心里，我自己先要信它——而信了它的人，出手就没轻没重了。我还没到那一步。',
'碎魂一脉自述：不伤皮肉直进识海，护身法宝一概没用；人身好护魂难护——这句他写在帖子上而不写进心里。', 141, '功法', '碎魂大法,摄魂,天南散修', 1260, 1, 74, 33, 0, 1),

(1042, '天极玄光：我叫三个晚辈先出去',
'在下鲁卫英，天极门长老。这一篇写在坠魔谷之后——我还喘着气，就趁还喘着写几句。\n\n【主修功法】\n天极玄光，不显不抢，先护同行的人。这一门光不出奇，长处是密，密到能替人挡一息。\n\n【能力自述】\n长老的一剑出手极老，看着不像元婴，可它到位。我不求胜、不求债，先把谷口给你堵住——护住后撤，这四个字是我这一辈子最拿得出手的神通。\n\n【一句实话】\n谷里那东西不是我们杀得死的。我能做到的，是叫三个晚辈先出去。天极门这次进去的人只回来几个，我算哪个没回去的。若你读到它，那大概是我回去了。',
'天极门长老写在坠魔谷之后：天极玄光不显不抢先护同行，杀不死谷里那东西，他能做的是叫三个晚辈先出去。', 142, '见闻', '天极玄光,坠魔谷,护后撤', 1210, 1, 90, 36, 0, 1),

(1043, '御灵真诀：兽不听我的，只听划算',
'在下东门图，御灵宗大长老。\n\n【主修功法】\n御灵真诀：灵虫猛兽皆受我调。别人以为御兽是靠禁制压服，我说句实话——兽不听我的，只听划算。我这一生做的事就一件：把它们算明白。\n\n【能力自述】\n灵虫群不咬人，先吃你法器上那层灵光，吃完了再谈。唤兽上山：我不动手，我吹一声，山上那些东西替我动。御灵印在身上，三阶以内的东西近不了。\n\n【一句实话】\n御灵宗不靠人多。我这一门最要紧的本事不是能打，是能叫一群比你活得短、比你算得少的东西，心甘情愿替你去拼命。这就是“划算”两个字值多少。',
'御灵宗大长老自述：兽不听只听划算，灵虫先吃法器灵光、唤兽上山替他动；这门最要紧的不是能打，是叫它们甘愿拼命。', 143, '神通', '御灵真诀,御兽,大长老', 1280, 1, 82, 37, 0, 1),

(1044, '火龙真炎：我叫童子，因为这张脸不会老',
'在下火龙童子，天南魔道散修。这一身年纪都长在心里了，脸没有。\n\n【主修功法】\n火龙真炎：以火养气，气尽火不灭。一气化龙，龙过成灰——看着热闹，其实烧的是你自己的水气。\n\n【能力自述】\n童子拜火最扰人：一张小孩的脸站在火前，你那一剑递不递得下去，就在这三息里见分晓。以身为炉，法术近身先被烧去一半。火龙焚天是拼命一手：一口气养了三百年的火出窍那一刻，我自己也要少活十年。\n\n【一句实话】\n我叫童子，不是装嫩，是这张脸不会老，索性拿它当兵器。魔道里活久了的人都懂：叫人先小看你三分，比多一件法宝好用。',
'魔道散修自述火龙真炎：一张小孩脸站在火前扰人三息，以身为炉、法术近身先烧一半；叫人小看你三分比多件法宝好用。', 144, '功法', '火龙真炎,魔道散修,火修', 1300, 1, 77, 34, 0, 1),

(1045, '白虹一剑：独斗过十级妖狼，靠的是不讲体面',
'在下白瑶怡，天南散修，一人一剑，不进任何一盟。\n\n【主修功法】\n白虹剑诀。白虹一剑，剑光起时像一道虹——虹短，够用。\n\n【能力自述】\n剑光分影，三道虚影一张网；那张网不是它撒的，是我撒的。我不硬拼，且战且走——十级的爪子擦身而过，靠的不是挡，是让它抓偏。虹化是最后一步：把自己炼成一线光，这一线过去，我与剑只剩一个还能站着。\n\n【一句实话】\n当年在外头独斗过一头十级妖狼，游刃有余是外面传的虚话，我那时狼狈得很。可跟妖物打过的人都懂：它不跟你讲规矩，所以你也不必讲体面。我这一身剑就是这么磨出来的。',
'天南顶尖元中女修自述白虹一剑：一人一剑不进任何盟；独斗十级妖狼游刃有余是虚话，靠的是它不讲规矩她也不讲体面。', 145, '功法', '白虹剑诀,女修,剑修', 1410, 1, 98, 44, 0, 1),

(1046, '落云宗镇宗功法：掌门是最后一个走的',
'在下程天坤，落云宗掌门。论修为，我在几位峰主之下——掌门从来不是宗里最强的那个。\n\n【主修功法】\n落云宗镇宗功法，不求胜人，只求不丢宗门。掌门印借阵：这一击不是我打的，是落云宗三座峰一起打的。\n\n【能力自述】\n护山禁制封一门：峰主们在里头议事，我在外头把门关上。镇宗厚气出不了重手，长处是站着不倒。宗门之重这一手，吃的是一宗的灵气。\n\n【一句实话】\n掌门难的地方不在能打，在于散会之后要断谁是谁非，在于山门要破的时候你是最后一个走的那个。几位峰主比我强，可他们砍人的时候，得有人在后头撑着那面旗。那面旗归我。',
'落云宗掌门自述：论修为在几位峰主之下，掌门不是最强的是最后一个走的；他们砍人的时候，得有人在后头撑那面旗。', 146, '宗门', '落云宗,镇宗功法,掌门', 1240, 1, 72, 32, 0, 1),

(1047, '千浪决：浪多不代表浪大，靠的是打不完',
'在下吕洛，落云宗长老。\n\n【主修功法】\n千浪决：一波未平，一波又起。千浪叠涌，一击之后还有九击，每一击都不重——加起来就重了。\n\n【能力自述】\n浪多不代表浪大，我这一门靠的是打不完。浪里抽身：水气一乱，人已经在三十丈外。一层水幕不挡重手，只把你那一击偏出去半尺。叠了百年的浪一起碎在你身上——声音很大，威力其实一般，可你得先扛住那声音。\n\n【一句实话】\n掌门说落云宗的旗他撑。我不管旗，我管磨人。同境界里谁碰上我这一身千浪，赢了也是脱层皮。打不死人也熬死人——这就是我这一门活下来的道理。',
'落云宗长老自述千浪决：一波未平一波又起、一击之后还有九击；浪多不代表浪大，靠的是打不完，赢了也脱层皮。', 147, '功法', '千浪决,落云宗,长于遁走', 1180, 1, 68, 30, 0, 1),

(1048, '元磁神光：两极之中，我养阳极',
'小女子温青，天星双圣之一，人称凌夫人。天灵根不是我选的，双修也不是——可这一身两极，我认。\n\n【主修功法】\n元磁神光。此光分两极，一人身上养不齐，我与夫君各修其一，合之则成。他养阴，我养阳。\n\n【能力自述】\n摄你兵：你祭出的那件东西此刻在半空中偏了一线——一线就够。阳极一放需两极同时起、同时收，三百年练的就是这一口气。星宫镇海阵的半边阵眼在我身上，我这一半今日不许丢。\n\n【一句实话】\n外人都道双圣是夫妻恩爱。恩爱是一回事，气机同频是另一回事——谁状态不好两人一起停下，这条比什么誓言都难守。',
'天星双圣之一自述：元磁神光分两极、他养阴她养阳，三百年练的就是同时起同时收那一口气；谁状态不好两人一起停下。', 148, '双修', '元磁神光,天星双圣,天灵根', 1520, 1, 110, 51, 0, 1),

(1049, '阴罗宗宗主：最听话的是我这三具尸王',
'在下房某，大晋阴罗宗宗主。宗门里的事不必多说，说多说少都是照办。\n\n【主修功法】\n阴罗宗炼尸大法，随身三具尸王——铜甲、乌甲各是其主。我门下最听话的就是这三具：它们不讨价，也不告老。\n\n【能力自述】\n铜甲尸王上前，不知痛不惧死，生前也是元婴，死后比我听话。两具同使，一正一侧，你顾哪一头？金色尖锥遁速极快：宗主不与人斗久，要么尸王上，要么我这一锥先走。\n\n【一句实话】\n灭仙珠还在匣子里。此珠一发，境界这个东西今日不算——可代价是三具尸王都要替我按住它。我留它到现在，不是舍不得用，是还没到叫我舍得三条命的份上。',
'阴罗宗宗主自述：最听话的是三具尸王，不讨价不告老；灭仙珠还在匣子里，不是舍不用，是还没到舍得三条命的份上。', 149, '功法', '阴罗宗,炼尸大法,尸王', 1460, 1, 101, 46, 0, 1),

(1050, '逆情断天：先把你心里那点东西砍了',
'老夫乾某，阴罗宗大长老。逆情断天这四个字不是形容，是一条门槛：先把你心里那点东西砍了，方敢入门。\n\n【主修功法】\n逆情断天大法。五子同心魔是顶峰——五个元婴精魂祭炼二十年，合起来是一具不死的魔，合体可达元后巅峰而不灭。\n\n【能力自述】\n鬼罗幡一展开，里头那些精魂都认识我——因为它们全是我放进去的。血魔珠里还养着一个元婴，那是我的第二副肉躯，这具坏了不要紧。我不怕你威胁我：我这一身已经没有可以被威胁的地方了。\n\n【一句实话】\n砍掉的那部分会疼三百年。疼完了，力气都是它的。一个连自己心都砍了的人，你让他拿什么讲条件。',
'阴罗宗大长老自述：逆情断天是一条门槛先砍掉心里那点东西，五子同心魔合体不灭；砍掉的那部分会疼三百年，疼完力气都是它的。', 150, '功法', '逆情断天大法,五子同心魔,魔道', 1490, 1, 96, 43, 0, 1),

(1051, '平山印：灵宝不是我们的，我们只替它看门',
'在下玄青子，大晋太一门话事人。太一门不擅斗狠，擅的是不被斗狠。\n\n【主修功法】\n太一玄功 + 平山印。这印是照着真正的通天灵宝仿的。我常对门下说一句话：灵宝不是我们的，我们只是替它看门。\n\n【能力自述】\n平山印一落下来不带风声——山不是被打倒的，是被按住的。印未落，你手上那件先颤，它认得比它高的东西。仿宝之威，先压器。\n\n【一句实话】\n山不至，人已平。外头有人说太一门仗着一件仿制品，说得对。可一件仿制品能叫大晋几家元后坐下来听我讲理，这里头的道理，比法宝本身值得琢磨。',
'太一门话事人自述：平山印照着通天灵宝仿成，山不是被打倒的是被按住的；一件仿制品能叫几家元后坐下讲理，道理比法宝本身值钱。', 151, '法宝', '平山印,太一门,通天灵宝', 1410, 1, 92, 42, 0, 1),

(1052, '赤鸣鼓：一鼓破胆，再鼓散魂',
'在下七妙，大晋天魔宗执法长老。执法这件事不能讲情面，也不能讲力气——讲力气就轮不到我。\n\n【主修功法】\n赤鸣鼓音，此鼓也是仿制灵宝。鼓声不进耳，进心脉：你听见那一下，胆气已经短了三分。一鼓破胆，再鼓散魂——两鼓之间没有第三鼓的余地。\n\n【能力自述】\n宗里有人要动手，我先敲一鼓；鼓响之后还动手的，才叫执法。三鼓之下万军自乱。\n\n【一句实话】\n赤鸣鼓一响，“仿制灵宝”四个字就没人敢提了。我这身修为在元后里排不到前头，可宗里议处罚、议拿人的时候，都得先看鼓在不在我手上。执法长老的本钱不在拳头上，在那条文上。',
'天魔宗执法长老自述赤鸣鼓：鼓声不进耳进心脉，一鼓破胆再鼓散魂；执法长老的本钱不在拳头上，在那条文上。', 152, '法宝', '赤鸣鼓,执法长老,鼓音', 1330, 1, 85, 39, 0, 1),

(1053, '血凄真解：别人炼气，我炼血',
'老夫坤无极，大晋血凄门大长老。别人炼气，我炼血。血少一分，力气就多一分——这笔账我自己算得清。\n\n【主修功法】\n血凄真解：以自身精血换一时之强。一口精血喷在法器上，那一击立刻重三成。重的这三成，是从寿元里借的。\n\n【能力自述】\n这一门不养气养血，血回来了人才算回来。摄血成幕不挡法，挡的是你靠近的那一步。一门之血尽付此击——今日之后我要养六十年，养不养得回来，看你死没死。\n\n【一句实话】\n血炼的路子看着风光，其实是拿命换眼前的力气。我这条老命剩下的不多，与其攒着这点寿元慢慢枯死，不如烧在一次值得的出手上。',
'血凄门大长老自述：别人炼气我炼血，一口精血喷法器那一击重三成、是从寿元里借的；不如把这具老命烧在一次值得的出手上。', 153, '功法', '血凄真解,以血换力,精血', 1280, 1, 79, 35, 0, 1),

(1054, '明王诀：金身一现，法不辩、理不辩',
'老衲元智，大晋雷音宗。修明王诀与四层金刚诀，一具肉身练得堪比法宝。\n\n【主修功法】\n明王诀，金身四相。金诀练到第四层，刀砍上去是砍在自己法器上。禅杖上那颗万年火珊瑚，不是我供的，是我拿来敲人的。\n\n【能力自述】\n明王忿怒相：一念忿怒，一身金相。雷音宗不动这相，动这相就是不讲佛法了。法力金刚召出的不是佛，是老衲这些年攒下的那点法力，它比我耐打。\n\n【一句实话】\n出家人该讲慈悲。可慈悲不是没有牙。金身一现那一刻，法不辩、理不辩——老衲这一生只辩这一次，辩完照样回去念我的经。',
'雷音宗高僧自述明王诀：金身四相肉身堪比法宝，火珊瑚不是供的是拿来敲人的；慈悲不是没有牙，金身一现法不辩理不辩。', 154, '功法', '明王诀,金刚诀,雷音宗', 1350, 1, 88, 40, 0, 1),

(1055, '净火禅定：没人见过我出手，所以人人都说我最强',
'老衲碧月，大晋净火宗。四百年前入元后，宗门说这是万年一遇。没人见过老衲出手，所以人人都说老衲最强。这一句我受着，受了几百年。\n\n【主修功法】\n净火禅定。此火不焚人，先焚自心。火不烧皮肉，先烧你身上那些不干净的东西——魔道修士最怕这一味。\n\n【能力自述】\n禅定不动：四百年的静坐不是白坐，你那一击到我身前，我先不动心。火中生莲，伤势用自家真火养，疼是真疼，好也是真好。\n\n【一句实话】\n那一手我一直没用。今日若出，往后四百年大约又要从头再来。所以你还是别逼我——不是怕输，是舍不得那四百年的安静。',
'净火宗禅师自述：没人见过他出手所以人人都说他最强，那一手一直没用——不是怕输，是舍不得那四百年的安静。', 155, '功法', '净火禅定,四百年,禅定', 1310, 1, 83, 38, 0, 1),

(1056, '洗天剑诀：散修没有靠山，我的靠山是那一剑',
'在下易洗天，大晋四大散修之一。这个名号不是封的，是一路活下来别人叫的——叫的人多，就得应。\n\n【主修功法】\n洗天剑诀，一人一剑，不属任何宗门。对皮粗肉厚的东西，剑要走在它眨眼之前。八级妖兽我斩过，它也没眨眼。\n\n【能力自述】\n洗天剑气铺开像下雨。散修的剑不讲究漂亮，讲究覆盖。散修的警觉比剑诀值钱：我睡觉时护身灵光也不散。\n\n【一句实话】\n富成那句“战力不宜高估”我听过，说得公道。散修不像宗门弟子背后有一整座山。我能站着讲这句话，靠的不是名号，是那一带八级妖兽真被我斩过。',
'大晋四大散修之一自述洗天剑诀：八级妖兽真斩过、剑走在它眨眼之前；散修背后没有山，能站着讲规矩靠的是那一剑。', 156, '功法', '洗天剑诀,散修,八级妖兽', 1400, 1, 93, 42, 0, 1),

(1057, '乾蓝冰焰：北夜的冰不是寒，是慢',
'老衲寒骊，大晋北夜小极宫。两口乾蓝鼎随身，乾蓝冰焰这一路修到了头。\n\n【主修功法】\n乾蓝冰焰。这火焰看着蓝，其实不烫——它冻的是你运功的那条线。北夜的冰不是寒，是慢：慢到你身上那点灵气忘了自己是活的。\n\n【能力自述】\n两口乾蓝鼎一起祭出，一口压你身，一口装你那些没了用的东西。冰焰护体，近我三尺者先僵——僵那一息，够我把鼎收回来。\n\n【一句实话】\n这一场我只当是替小极宫看门，门里的东西你不能带走。北夜苦寒，冰焰是一步步熬出来的；我这张脸也冻得没什么表情——不是冷，是习惯了把力气省在出手那一下。',
'北夜小极宫寒骊上人自述乾蓝冰焰：冻的不是身是你运功那条线，北夜的冰不是寒是慢；他轻接三焰一扇，守住了小极宫的门。', 157, '功法', '乾蓝冰焰,小极宫,冰修', 1440, 1, 97, 45, 0, 1),

(1058, '小极宫主：制住她的不是功法，是那把钥匙',
'在下北夜小极宫主。外界传我这一宫有不少秘辛，这里只讲一件：我手里有一把黄泉的钥匙。\n\n【主修功法】\n小极宫主功法，兼一门制约黄泉鬼母之法。我不擅杀伐，擅的是叫一个本该撒手的人，不敢撒手。\n\n【能力自述】\n钥令一亮，自有人替我出手——那个不是人。小极寒冰阵不靠阵旗，靠这一山的寒气，我站在阵心就是阵眼。宫主之位这一手不为挡人，是因为一宫的人都在我身后，我不能散。\n\n【一句实话】\n制得住鬼母的不是我的功法，是那把钥匙。这一句我从不避。钥匙只有一把，今日用它，明日宫里就要少一个人——这买卖我一直在算，也一直不敢真算清。',
'小极宫主自述：制住黄泉鬼母的不是功法是那把钥匙，钥令一亮自有人替她出手；钥匙只一把，用一次明日宫里就少一个人。', 158, '神通', '小极宫,钥令,制约鬼母', 1320, 1, 86, 40, 0, 1),

(1059, '黄泉鬼母：最不值钱的就是我这具身子',
'他们叫我鬼母。北夜黄泉白板鬼修，无肉身，纯以阴魂作战。\n\n【主修功法】\n黄泉鬼道。我不杀你——我进你那具身子，试试合不合穿。黄泉雾里那些脸都是我的旧识，它们在帮你选一边站。\n\n【能力自述】\n无身可伤：你砍了我三剑，我少了一层雾。这话听着像认输，其实是认我这具身子不算。\n\n【一句实话】\n我这一身最不值钱的就是那具身体，它早烂了。所以你们那些拿肉身、拿法宝、拿名头讲价的路数，对我一概不灵。被制了三百年，阴魂不散——今日谁把钥匙给我，我替他做一件事；不是我好说话，是我这副样子，除了做事再没别的东西可拿出去换。',
'北夜白板鬼修自述：最不值钱的就是那具烂了的身子，拿肉身法宝名头讲价对她一概不灵；除了做事，再没别的东西可拿去换。', 159, '功法', '黄泉鬼道,白板鬼修,阴魂', 1290, 1, 81, 37, 0, 1),

(1060, '黑血刃：世家靠的是肯把精血拿出来的人多',
'老夫叶某，大晋叶家大长老。论资排辈这一族我不算最能打的，可我站在最前头。\n\n【主修功法】\n黑血刃。这口刃要吃血才亮——血是自己的，亮一分就是一分。\n\n【能力自述】\n那一击古魔我接了半边身子。半边够了，身后那些孩子还没跑远。喝退妖禽：四翅的畜生不怕人，怕的是敢拿刀跟它讲价的人。我曾拿这口刃砍伤过一头十级妖禽，此事叶家记进了档。\n\n【一句实话】\n世家不是功法好，是肯把精血拿出来的人多。叶家这一代能站着说话，是因为上一代有人肯把血放完。到我这儿，我也一样放——这把刃亮到几时，我这条命就押到几时。',
'叶家大长老自述黑血刃：刃要吃血才亮，古魔那一击他接了半边身子；世家不是功法好，是肯把精血拿出来的人多。', 160, '功法', '黑血刃,叶家,世家', 1380, 1, 92, 42, 0, 1),

(1061, '藏了三百年：族里都当我修为平平',
'在下叶月圣，叶家行七，族里孩子叫我七叔。平日帮着看看孩子，修为平平——这话我任他们说了三百年。\n\n【主修功法】\n借古魔之力炼体。这一步最疼：要把自己改成一具能借得力气的身子。每借一分，我就少记得一点自己原来的名字。\n\n【能力自述】\n藏了三百年的一拳不出去，我还是那个看孩子的七叔；这一拳一旦出去，就不是了。\n\n【一句实话】\n我藏得比谁都深，深到连自己都差点信了。今日之后，叶家没有七叔了。这一句我等了很久，也怕了很久。怕的不是动手，是动手之后族里那几个孩子看我的眼神——三百年和气，原来是一层随时要破的皮。',
'叶家七叔自述：平日看孩子、修为平平任人说了三百年；藏的那一拳一旦出去他就不再是七叔，三百年和气原来是一层随时要破的皮。', 161, '功法', '叶家,隐藏修为,借古魔炼体', 1350, 1, 90, 41, 0, 1),

(1062, '九幽宗执法长老：办完了，不必说',
'在下富成，大晋九幽宗执法长老。宗门里的事，办完了不必说——说了就还得再办一次。\n\n【主修功法】\n九幽玄功，不显山不露水，专办宗门的事。九幽一路最会拿人心，我这手护住本心，专门防自家的人心。\n\n【能力自述】\n九幽拿禁不伤人，先把你两只手借走——宗里办事讲个规矩。执法一击不重，重的是它背后的那条文。\n\n【一句实话】\n我办过的人里，有一半今日会替我说话，这就是执法长老的本钱。易洗天那位我说过“战力不宜高估”——不是压他，是这一行的账得有人平着算。平了，宗门才散不了。至于人怎么看我，不重要。',
'九幽宗执法长老自述：九幽最会拿人心，他专防自家的人心；执法一击不重，重的是背后那条文——办完了就走，人怎么看他不重要。', 162, '宗门', '九幽宗,执法长老,九幽玄功', 1220, 1, 76, 34, 0, 1),

(1063, '天澜草原大仙师：草原不认宗门，认祭坛',
'老夫徐某人，天澜草原大仙师。与至阳上人、乾老魔齐名这话，是草原人抬举我。\n\n【主修功法】\n草原祭天大法。借地势祭坛，不借人身。我这一击不是从手上来的，是从脚下那片地来的。\n\n【能力自述】\n大仙师之名在草原上有分量，分量本身就能压住一半人。坛上那点烟一升，万帐的人都站我这边。躯体不全也要硬撑阵——因为我一散，万帐就乱。\n\n【一句实话】\n草原认祭坛不认宗门，这座坛是草原几百年的账。我这条命后来折在一个兽手里，被砍去了半边躯体——我不怨，站在这种位置上的人，本来就没打算全身而退。',
'天澜草原大仙师自述：祭天大法借地势祭坛不借人身，一散万帐就乱；这条命折在兽手被砍去半边躯体也不怨——本就没打算全身而退。', 163, '功法', '祭天大法,天澜草原,祭坛', 1310, 1, 84, 38, 0, 1),

(1064, '慕兰第四神师：神来那一瞬，我不是我',
'在下田锤，慕兰草原第四神师，刚进阶元后不久。草原上的神师，讲的是一个“神来”。\n\n【主修功法】\n慕兰巫神功：以血祭神，神力附体。血一放出去，我就得在坛下挨着。\n\n【能力自述】\n血祭请神——神来那一瞬，你的法诀会自己乱一拍，一拍够了。神师一击我使不出自己的力气，使的是附在我身上那一份。被冰牵住我就不换刀：这一门神功教的头一条是不喊疼。\n\n【一句实话】\n神不来说我不在。当年北夜那位把我冻住牵了一场，我认。神来一瞬只借一次，那一瞬里我不是我——最好是因为那一瞬我强，最坏是因为那一瞬过了，我还得做回我自己。',
'慕兰第四神师自述巫神功：血祭请神、那一瞬里我不是我，法诀自己乱一拍；只借一瞬，过了还得做回自己——这是最好的，也是最坏的。', 164, '功法', '慕兰巫神功,血祭请神,神师', 1270, 1, 80, 36, 0, 1),

(1065, '蛟龙族长：外海不认人界的规矩，只认鳞片的硬度',
'本王金蛟，乱星海外海蛟龙族长，十级妖修。人界的规矩到了外海就换了个算法。\n\n【主修功法】\n蛟龙族炼体 + 金甲长枪。枪是蛟筋做杆、龙牙做尖——人界那些法器在它面前像纸糊的。一片鳞就是一件护身法宝，我身上有几千片。\n\n【能力自述】\n我一枪不出先叫：海里有的是愿意替我出枪的。外海不认人界的座次，只认鳞片够不够硬。\n\n【一句实话】\n现出原身那一刻，人界的斗法就变成一个词：量。你们比功法、比法宝、比底牌；我们比的是谁活得久、谁长得硬。我在外海熬到十级，靠的不是聪明，是这一身熬得比谁都结实。',
'外海蛟龙族长、十级妖修自述：人界法器在它面前像纸糊，外海只认鳞片硬度；人界比功法法宝底牌，外海比谁活得久、谁长得硬。', 165, '神通', '金蛟王,十级妖修,外海', 1430, 1, 99, 44, 0, 1),

(1066, '万妖谷副谷主：这个位子是一刀一刀让出来的',
'老夫万年尸熊，天南万妖谷副谷主，十级妖修。谷里叫我二当家——这个位子不是打来的，是大家伙都挨过我一刀之后让出来的。\n\n【主修功法】\n尸熊之躯 + 血刃。这口血刃是仿制的魔龙刃，砍人不用第二下。仙师的躯体也是一刀。\n\n【能力自述】\n法器我都嫌麻烦，熊抱住之后你自己会散。这一身不是甲，是万年慢慢长出来的肉，它自己会夹住你的剑。\n\n【一句实话】\n昆吾山那一夜我没让过路。不让路是谷里的规矩——副谷主这个东西，听着像二把手，其实是叫所有人知道：真要动手，第二关是我。我这身力气不聪明，可它够沉，沉到没人愿意来试第二次。',
'万妖谷副谷主自述：这个位子不是打来的，是大家挨过他一刀之后让出来的；昆吾山那一夜他没让过路——真要动手，第二关是他。', 166, '神通', '万年尸熊,血刃,万妖谷', 1390, 1, 95, 43, 0, 1),

(1067, '青背苍狼：我穿人衣说人话，只为叫人先不想动手',
'在下青背苍狼，万妖谷副谷主，十级妖修。如今作个皂袍老者模样。\n\n【主修功法】\n苍狼血脉 + 皂袍藏网。这张网不勒人，勒法器：网住的那一位今日只能用自己的手。\n\n【能力自述】\n狼的打法只有一式：先绕，后扑——绕的那三圈你也得算进去。两个元中一起上来我自然分开打，分开打靠腿。当年一张网困住过两位元中。\n\n【一句实话】\n我穿人衣、说人话，学人模人样。不是为了像人，是为了叫人先不想动手——愿意先跟我坐下来讲两句的，往往就少打一场。谷中狼群齐至：我不是最能打的，可我叫得动的比他们多。',
'万妖谷副谷主青背苍狼自述：这张网不勒人勒法器，先绕后扑分开打靠腿；穿人衣说人话不是为了像人，是叫人先不想动手。', 167, '神通', '青背苍狼,万妖谷,藏网', 1330, 1, 87, 39, 0, 1),

(1068, '昆吾山古木：我不追人，我长得比你跑得远',
'在下木魁，昆吾山一株万年古木，十级妖修。我不会说话利索，就这么写。\n\n【主修功法】\n草木大阵。山上那些草都是我的根，你踩了一路，其实一直在我身上。根须延数十里，木雾含剧毒——雾不难看，就是不能呼吸。\n\n【能力自述】\n一根枝条就是一件法宝，我身上有几万根。烧了我一半不要紧，根在，半座山替我长回来。我不追人，我长得比你跑得远。\n\n【一句实话】\n我这一式不是法术，是年岁。一万年的东西压下来，你拿什么挡？我不急，急的是你们——寿元短的人等不起，我等得起。你上山那一刻起，就已经在我身上了。',
'昆吾山万年古木自述草木大阵：山上的草都是它的根，踩一路其实一直在它身上；它不追人，长得比人跑得远，比的是年岁。', 168, '神通', '木魁,草木大阵,万年古木', 1280, 1, 82, 37, 0, 1),

(1069, '银翅夜叉：想事的尸，才是最难杀的那种',
'在下银翅夜叉，昆吾山阴阳窟一具飞天尸，十级，仅次于金身月尸。他们以为尸不会想事——想事的尸，才是真正难杀的那种。\n\n【主修功法】\n飞天尸神通：煞魂丝、风土二遁，兼修幻术。此丝不缚身，缚你元婴；婴被缚住，你那具身子就只剩一层皮。\n\n【能力自述】\n阴阳窟里我秒杀过两名元中，没用第二招——不是省力气，是叫他们以为还有第三人在洞内。尸身不需要呼吸，土遁我能多待半日。\n\n【一句实话】\n翅一开，窟中尽是银光。哪一只是真的，你得自己猜。做尸做到我这份上，靠的不是硬，是肯动那点别人以为尸不会动的念头。',
'昆吾山十级飞天尸自述：煞魂丝不缚身缚元婴，秒杀两元中靠的是叫他们以为还有第三人；做尸靠的不是硬，是肯动那点念头。', 169, '神通', '银翅夜叉,飞天尸,昆吾山', 1350, 1, 90, 41, 0, 1),

(1070, '昆吾山四翅：我不会说话，所以没人劝我讲道理',
'……（不会写人话。它叫狮禽兽，昆吾山四妖之一，狮首鹰身四翅，还没化形。）\n\n【它这一身】\n它不修神识，只修力。四翅一齐，雷在爪上——它不快到看不见，只是挡不住。一声金色波纹下去，山石齐碎。\n\n【它打法】\n它不使遁术，它就是飞，可四翅一展你已经算不出它下一步落在哪儿。它不懂法诀也不懂克制，只会把全身力气放在一下里——这一下比多数元后的神通都重。\n\n【一句】\n它不会说话。山上的人都知道，所以从不劝它讲道理。讲道理的仗它打不来；不讲道理的，它一向赢得。',
'昆吾山四妖之一、狮首鹰身四翅未化形：不修神识只修力，四翅一齐雷在爪上；不会说话所以没人劝它讲道理，不讲道理的仗它一向赢得。', 170, '神通', '狮禽兽,昆吾山,未化形', 1240, 1, 78, 35, 0, 1),

(1071, '万妖幡主：幡里那些东西都欠我',
'在下万妖幡主，人称车老妖，天南万妖谷一系，十级妖修。\n\n【主修功法】\n万妖幡。一幡在手，里头几百头。它们不听话，但它们怕我比怕你多。幡里那些东西都欠我——欠得久了，它们就肯替我死。\n\n【能力自述】\n化身代战：你打赢的那个不是我。这一条我从不隐瞒，因为说出来你也没办法。当年我这具化身与北夜黄泉鬼母斗了个不分胜负。\n\n【一句实话】\n万妖幡抖完，往后的年月里我得重新收一批。所以这一幡我轻易不抖到底——收妖这个东西，欠着比还清了好。还清了，它们就没理由替我死了。',
'万妖幡主自述：一幡几百头不听话但怕他比怕你多，化身代战打赢那个不是他；收妖欠着比还清好，还清了它们就没理由替他去死。', 171, '神通', '车老妖,万妖幡,化身代战', 1300, 1, 84, 38, 0, 1),

(1072, '虚天殿旧怪：死过一次，什么都可以不要了',
'在下玄骨上人，乱星海一座虚天殿里出来的旧怪。这座殿的账，我拿命记着。\n\n【主修功法】\n玄骨魔功：骨为器，魂为引。骨头是自己的，法器也是自己的——这一门里这两件事没分别。\n\n【能力自述】\n当年极阴祖师打爆我肉身、擒下我元婴，关了多年。如今我反手学了擒婴这一手，拿来还他——这话我不对人讲，只对自己讲。你吓不住一个已经死过一回的人。\n\n【一句实话】\n在那座殿里被关了多年，重见天日那一刻我先吃一口。死过一次以后，什么面子、什么势，都可以不要了。老怪复出没有什么宏图大愿，只有一件事：把当年那口气，原样咽回去。',
'虚天殿旧怪自述：当年肉身被打爆、元婴被擒关了多年，如今反手学擒婴拿来还他；死过一次以后，面子与势都可以不要了。', 172, '功法', '玄骨魔功,虚天殿,复出', 1320, 1, 86, 39, 0, 1),

(1073, '结丹第一人：一身宝物，一身劫',
'在下温天仁，乱星海结丹修士第一人，六道极圣门下。师父那一门的功，我使了个皮。\n\n【主修功法】\n六极真魔功，另有化劫大法、颠凤培元功、引龙诀。我身上宝物最多——天阳鎏金针是为噬金虫专门炼的，八门金光镜是仿品。我身上全是这种“专门”。\n\n【能力自述】\n化劫大法的要诀是断肢：劫来了就切一条手臂给它，切过一次下次还敢切。遇上辟邪神雷，我那六气就只够挨一下——这个我心里有数。\n\n【一句实话】\n身上宝物最多的人，最后死在一条不会法术的手臂上。这不是说我，是说那一夜的我：被鬼雾卷进阴冥之地，法力尽废。八门金光镜八门齐开那一刻，我真以为自己已经是元婴——每一道金光，都要先耗我自己一口元气。',
'乱星海结丹第一人、六道极圣门下自述：一身宝物全是专门炼的，化劫靠断肢；可身上宝物最多的人，最后死在一条不会法术的手臂上。', 173, '法宝', '温天仁,六极真魔功,化劫大法', 1470, 1, 103, 47, 0, 1);

-- 第三批互评：每篇恰 1 条（1053-1095，与上面每篇 comment_count=1 严格对齐），沿人物关系写。
INSERT IGNORE INTO blog_comment (id, article_id, user_id, parent_id, content, status) VALUES
(1053, 1031, 132, 0, '魏老怪的毒摆到明处，反倒比某些笑着递刀的正道干净。这话我不该替你说。', 1),
(1054, 1032, 134, 0, '至阳道友肯当众认自己最弱，这份稳，我三百年学不会。你那座阵我进过一次，没讨到好。', 1),
(1055, 1033, 149, 0, '合欢兄讲效果不讲正邪，说得洒脱。改日再切磋一场，这回我带齐三具尸，你也别只用一气化三。', 1),
(1056, 1034, 132, 0, '记了三百年那口气，你今日原样还我一次便算两清。往后各自守各自那道门，别再动手。', 1),
(1057, 1035, 136, 0, '盟里的账你从不算，都留给我算。写得倒像你不牵头似的。下一战，凤龙合鸣还照旧。', 1),
(1058, 1036, 135, 0, '海上的账你平，盟里的人心我聚。两下凑齐才叫鸾鸣宗。当年那位神师被你牵住那一场，我记着。', 1),
(1059, 1037, 101, 0, '前辈那对风雷翅与一口乾蓝冰焰，我一生记着——哦，你才是那后进。丝尽剑亡那一步，早晚我再走一遍。', 1),
(1060, 1038, 102, 0, '同为南宫，一条道上没打过照面是真。谁的账谁自己还，这一句我认同。', 1),
(1061, 1039, 101, 0, '谷口那盏灯我年少时见过。前辈替黄枫谷看着后路，韩立不敢忘，只是当年没敢说。', 1),
(1062, 1040, 134, 0, '南陇兄退路就是宗门这话，散修都该刻在手上。坟头草三丈那句，我读着笑了半日。', 1),
(1063, 1041, 107, 0, '分识也怕识海窄。你那句写在帖子上不写进心里，与我一辈子怕多探那一寸，是同一桩心事。', 1),
(1064, 1042, 101, 0, '坠魔谷那一役晚辈也听过。你把三个晚辈先送出去，这一笔，比许多元婴的名头都重。', 1),
(1065, 1043, 123, 0, '兽只听划算，前辈说得比我们灵兽山还透。御兽与御灵差的这一个字，我琢磨了半辈子。', 1),
(1066, 1044, 133, 0, '童子这张脸当兵器的道理，魔道里都懂。改日教我一手法，我拿一气化三换你三息拜火，如何。', 1),
(1067, 1045, 130, 0, '它不讲规矩你就不讲体面——这句剑修该记。当年我抱孩子突围时若懂这一层，能少伤几分。', 1),
(1068, 1046, 128, 0, '掌门把撑旗这桩苦差认下了。峰主们砍人，你在后头撑着——落云宗还站着，一半是这个缘故。', 1),
(1069, 1047, 146, 0, '你说你不管旗只管磨人。可我偏要把你这一身千浪算进宗门的账——打不死也熬死人，正是守山要的东西。', 1),
(1070, 1048, 117, 0, '两极同时起、同时收，这一口气我们练了三百年。今日你一人也叫这片海听调——我不在，你也没丢。', 1),
(1071, 1049, 150, 0, '宗主那匣灭仙珠留着是对的。我五子同心魔不灭，你三具尸王听话——阴罗宗这一文一武，谁也别先拼命。', 1),
(1072, 1050, 113, 0, '你说擒婴锁得住别人锁不住自己因果。我逆情断天砍了自己的心，到头来一样躲不过。这话你我该各抄一份。', 1),
(1073, 1051, 116, 0, '一件仿制灵宝能叫几家元后坐下讲理，这里头的账，与星宫镇海阵一样：值钱的不是兵，是有没人肯认这条规矩。', 1),
(1074, 1052, 162, 0, '执法的本钱不在拳头在条文——这句九幽与天魔两宗都该供起来。鼓在谁手上，谁的话就重三分。', 1),
(1075, 1053, 149, 0, '血炼的人肯把老命烧在一次值得的出手上，我信。三具尸王替我按珠时，也是这股不计较的劲儿。', 1),
(1076, 1054, 155, 0, '慈悲不是没有牙。老衲念一句：金刚怒目，所以降伏四魔。你那一手舍不得出的安静，也是一种慈悲。', 1),
(1077, 1055, 154, 0, '四百年不出手，是懒，也是怕出了手就回不去那份静。你只辩一次，我连那一次都想省。', 1),
(1078, 1056, 162, 0, '我说你战力不宜高估，你肯认我公道——散修里这样的不多。斩过八级妖兽才讲没斩过的规矩，这道理也是我的。', 1),
(1079, 1057, 158, 0, '门里的东西不能带走，这一句你记着。三焰一扇那一场你替小极宫守住了，往后这两口鼎还压得住。', 1),
(1080, 1058, 157, 0, '那把钥匙我算不清，也只能装着装得下。你替宫拿钥匙，我替宫看门——北夜这一冬，还得接着过。', 1),
(1081, 1059, 158, 0, '制了你三百年的正是这一位。可你这副样子除了做事也没得挑——钥匙在谁手里，我便替你去做那件不该做的事。', 1),
(1082, 1060, 161, 0, '大长老别拦我。藏了三百年，皮迟早要破。那几个孩子若因此还能在叶家长大，这一下便值。', 1),
(1083, 1061, 160, 0, '七叔。平日看孩子这三年，族里都当你修为平平。可你肯把血放完给这一族，这份账我记着。', 1),
(1084, 1062, 152, 0, '办了的人一半替我说话——执法长老彼此都懂这一条。你在魔宗敲鼓，我在幽宗拿人，文是一样的文。', 1),
(1085, 1063, 164, 0, '大仙师那座坛烟一升，万帐都站你。我被北夜那位冻牵一场，才懂神来一瞬太短——你还得撑你的万帐。', 1),
(1086, 1064, 163, 0, '第四神师你血祭请神那一手，是拿你自己挨的疼换的——草原记你这一笔。', 1),
(1087, 1065, 109, 0, '外海的座次我听过，只认鳞片硬度这句不假。乱星海的潮汛年年闹，海里的账，本王与你这边迟早要对一回。', 1),
(1088, 1066, 167, 0, '二当家这个位子你我都是挨过刀才坐稳的。万妖谷的规矩是不让路——那一夜昆吾山上，我与你都站在最前。', 1),
(1089, 1067, 166, 0, '你血刃砍人不用第二下，我这张网勒法器。谷里一个唱硬一个唱绕，合起来才叫副谷主。', 1),
(1090, 1068, 126, 0, '省、慢、扛。这三个字木魁前辈比我更配。半座山都是你的根——我守了八百年山，最懂守山的人话不多。', 1),
(1091, 1069, 171, 0, '想事的尸最难杀。阴阳窟那一战我听过。改日叫你那对银翅与我万妖幡比比，看谁更能拖。', 1),
(1092, 1070, 168, 0, '……木魁也不大会说话。只说一句：四翅那一位，扑得狠，根在它脚下也认。山里的东西，不必多话。', 1),
(1093, 1071, 169, 0, '欠着比还清好——这一条你我两门是一样心思。你缚元婴，我养欠债的妖，说到底都在攒还不起那三个字。', 1),
(1094, 1072, 113, 0, '你反手学擒婴要还我。好。当年打爆你肉身、擒你元婴是我，这笔账我认。复出这口气你咽得下去便来。', 1),
(1095, 1073, 114, 0, '为师那一门的功你使了个皮，六气只够挨辟邪神雷一下——你自己心里有数，便还不算蠢。化劫断肢那一步是留着保命的，别拿它逞强。', 1);
