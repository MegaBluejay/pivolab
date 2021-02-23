import java.io.*;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PivoLab {

    private Map<Long, SpaceMarine> marines = new HashMap<>();

    private final String saveFilePath = System.getenv("PIVOFILE");

    long maxid = 0;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy");

    public void readFile() throws PivoFileException {
        InputStream stream;
        try {
            stream = new FileInputStream(saveFilePath);
        } catch (FileNotFoundException e) {
            throw PivoFileException.notFound();
        }
        InputStreamReader reader = new InputStreamReader(stream);
        String line;
        int i = 1;
        while (true) {
            try {
                if ((line = readLine(reader)).isEmpty()) break;
            } catch (IOException e) {
                throw PivoFileException.readProblem();
            }
            String[] fields = line.split(" *, *");
            if (fields.length != 12) {
                throw PivoFileException.nFields(i, fields.length);
            }
            long key;
            try {
                key = Long.parseLong(fields[0]);
            } catch (NumberFormatException e) {
                throw PivoFileException.invalidField(i, "key");
            }
            long id;
            try {
                id = Long.parseLong(fields[1]);
            } catch (NumberFormatException e) {
                throw PivoFileException.invalidField(i, "id");
            }
            String name = fields[2];
            double x;
            double y;
            try {
                x = Double.parseDouble(fields[3]);
            } catch (NumberFormatException e) {
                throw PivoFileException.invalidField(i, "x coord");
            }
            try {
                y = Double.parseDouble(fields[4]);
            } catch (NumberFormatException e) {
                throw PivoFileException.invalidField(i, "y coord");
            }
            Coordinates coordinates = new Coordinates(x, y);
            Date creationDate;
            try {
                 creationDate = dateFormat.parse(fields[5]);
            } catch (ParseException e) {
                throw PivoFileException.invalidField(i, "date");
            }
            float health;
            try {
                health = Float.parseFloat(fields[6]);
            } catch (NumberFormatException e) {
                throw PivoFileException.invalidField(i, "health");
            }
            AstartesCategory category;
            if (fields[7].equals("null")) {
                category = null;
            } else {
                try {
                    category = AstartesCategory.valueOf(fields[7]);
                } catch (IllegalArgumentException e) {
                    throw PivoFileException.invalidField(i, "category");
                }
            }
            Weapon weaponType;
            try {
                weaponType = Weapon.valueOf(fields[8]);
            } catch (IllegalArgumentException e) {
                throw PivoFileException.invalidField(i, "weapon type");
            }
            MeleeWeapon meleeWeapon;
            try {
                meleeWeapon = MeleeWeapon.valueOf(fields[9]);
            } catch (IllegalArgumentException e) {
                throw PivoFileException.invalidField(i, "melee weapon");
            }
            Chapter chapter;
            if (fields[10].equals("null")) {
                chapter = null;
            } else {
                String chapterName = fields[10];
                String world;
                if (fields[11].equals("null")) {
                    world = null;
                } else {
                    world = fields[11];
                }
                chapter = new Chapter(chapterName, world);
            }
            marines.put(key, new SpaceMarine(id, name, coordinates, creationDate, health, category, weaponType, meleeWeapon, chapter));
            i++;
        }
        maxid = marines.values().stream().map(SpaceMarine::getId).max(Long::compare).orElse(0L);
    }

    private static String readLine(InputStreamReader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        int ci;
        while ((ci = reader.read()) != -1) {
            char c = (char) ci;
            if (c == '\n') {
                break;
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private <T> void simpleSingleArg(String[] args, Function<String, T> parse, Predicate<T> isValid, String commandName, String argName, String validityErrorMessage, Consumer<T> action) {
        if (args.length == 1) {
            System.out.println(argName + " required");
        }
        else if (args.length > 2) {
            System.out.println(commandName + " only takes 1 same-line argument");
        }
        else {
            try {
                T t = parse.apply(args[1]);
                if (isValid.test(t)) {
                    action.accept(t);
                }
                else {
                    System.out.println(validityErrorMessage);
                }
            } catch (Exception e) {
                    System.out.println("invalid " + argName);
            }
        }
    }

    public void interact(Scanner scanner, boolean quiet) {
        if (!quiet) {
            System.out.print("> ");
        }
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] args = line.split(" +");
            if (args.length > 0) {
                String command = args[0];
                if (command.equals("help")) {
                    help();
                }
                else if (command.equals("info")) {
                    info();
                }
                else if (command.equals("show")) {
                    show();
                }
                else if (command.equals("insert")) {
                    simpleSingleArg(args,
                            Long::parseLong,
                            k -> !marines.containsKey(k),
                            "insert",
                            "key",
                            "key already present",
                            k -> insert(k, readMarine(scanner, quiet)));
                }
                else if (command.equals("update")) {
                    simpleSingleArg(args,
                            Long::parseLong,
                            id -> marines.values().stream().anyMatch(m -> m.getId().equals(id)),
                            "update",
                            "id",
                            "id not found",
                            id -> update(id, readMarine(scanner, quiet)));
                }
                else if (command.equals("remove_key")) {
                    simpleSingleArg(args,
                            Long::parseLong,
                            marines::containsKey,
                            "remove_key",
                            "key",
                            "key not found",
                            this::removeKey);
                }
                else if (command.equals("clear")) {
                    clear();
                }
                else if (command.equals("save")) {
                    try {
                        PrintWriter writer = new PrintWriter(saveFilePath);
                        marines.forEach((key, m) -> writer.println(
                                String.join(", ", key.toString(), m.getId().toString(), m.getName()
                                        , Double.toString(m.getCoordinates().getX())
                                        , m.getCoordinates().getY().toString(), dateFormat.format(m.getCreationDate())
                                        , m.getHealth().toString()
                                        , m.getCategory() == null ? "null" : m.getCategory().toString()
                                        , m.getWeaponType().toString(), m.getMeleeWeapon().toString()
                                        , m.getChapter() == null ? "null" : m.getChapter().getName()
                                        , m.getChapter() == null || m.getChapter().getWorld() == null
                                                ? "null" : m.getChapter().getWorld())
                        ));
                        writer.close();
                    } catch (FileNotFoundException e) {
                        System.out.println("problem with save file");
                    }
                }
                else if (command.equals("execute_script")) {
                    if (args.length == 1) {
                        System.out.println("file required");
                    }
                    else if (args.length > 2) {
                        System.out.println("execute_script only takes 1 argument");
                    }
                    else {
                        File scriptFile = new File(args[1]);
                        try {
                            if (Files.isReadable(scriptFile.toPath())) {
                                Scanner scriptScanner = new Scanner(new FileInputStream(scriptFile));
                                interact(scriptScanner, true);
                            }
                            else {
                                System.out.println("file not readable");
                            }
                        } catch (FileNotFoundException e) {
                            System.out.println("file not found");
                        }
                    }
                }
                else if (command.equals("exit")) {
                    break;
                }
                else if (command.equals("remove_lower")) {
                    if (args.length > 1) {
                        System.out.println("remove_lower doesn't take any same-line arguments");
                    }
                    else {
                        removeLower(readMarine(scanner, quiet));
                    }
                }
                else if (command.equals("replace_if_lower")) {
                    simpleSingleArg(args,
                            Long::parseLong,
                            marines::containsKey,
                            "replace_if_lower",
                            "key",
                            "key not found",
                            k -> replaceIfLower(k, readMarine(scanner, quiet)));
                }
                else if (command.equals("remove_lower_key")) {
                    simpleSingleArg(args,
                            Long::parseLong,
                            k -> true,
                            "remove_lower_key",
                            "key",
                            "",
                            this::removeLowerKey);
                }
                else if (command.equals("group_counting_by_creation_date")) {
                    groupCountingByCreationDate();
                }
                else if (command.equals("filter_greater_than_category")) {
                    if (args.length > 1) {
                        System.out.println("filter_greater_than_category doesn't take any same-line arguments");
                    }
                    else {
                        AstartesCategory category = readObject(scanner,
                                AstartesCategory::valueOf,
                                c -> true,
                                "Enter category (one of [" +
                                        Arrays.stream(AstartesCategory.values()).map(AstartesCategory::toString)
                                                .collect(Collectors.joining(", ")) + "]): ",
                                "invalid category",
                                false,
                                quiet);
                        filterGreaterThanCategory(category);
                    }
                }
                else if (command.equals("print_ascending")) {
                    printAscending();
                }
                else {
                    System.out.println("unknown command");
                }
            }
            if (!quiet) {
                System.out.print("> ");
            }
        }
    }

    private static void help() {
        System.out.println("all args written as {arg} must be specified on further lines");
        System.out.println("help print help");
        System.out.println("info print info about current state of marines");
        System.out.println("show print all marines");
        System.out.println("insert key {marine} add new marine with given key");
        System.out.println("update id {marine} update marine with given id");
        System.out.println("remove_key key delete marine with given key");
        System.out.println("clear delete all marines");
        System.out.println("save save marines to file");
        System.out.println("execute_script file_name execute script");
        System.out.println("exit end execution");
        System.out.println("remove_lower {marine} delete all marines with health lower than the one given");
        System.out.println("replace_if_lower key {marine} replace marine with key with given one if the new health is lower than the old");
        System.out.println("remove_lower_key key delete all marines with key lower than given");
        System.out.println("group_counting_by_creation_date print number of marines with each creation date");
        System.out.println("filter_greater_than_category {category} print marines with categories higher than the one given");
        System.out.println("print_ascending print all marines sorted by health");
    }

    private void info() {
        System.out.println("type: HashMap<Long, SpaceMarine>");
        System.out.println("number of elements: " + marines.size());
        if (!marines.isEmpty()) {
            System.out.println("newest marine created on " + marines.values().stream().max(Comparator.comparing(SpaceMarine::getCreationDate)));
        }
    }

    private static void printMarine(Long key, SpaceMarine marine) {
        System.out.println("Key: " + key);
        System.out.println("ID: " + marine.getId());
        System.out.println("Name: " + marine.getName());
        System.out.println("Coordinates: " + marine.getCoordinates());
        System.out.println("Creation date: " + dateFormat.format(marine.getCreationDate()));
        System.out.println("Health: " + marine.getHealth());
        System.out.println("Category: " + marine.getCategory());
        System.out.println("Weapon type: " + marine.getWeaponType());
        System.out.println("Melee weapon: " + marine.getMeleeWeapon());
        if (marine.getChapter() == null) {
            System.out.println("Chapter: null");
        }
        else {
            System.out.println("Chapter name: " + marine.getChapter().getName());
            System.out.println("Chapter world: " + marine.getChapter().getWorld());
        }
    }

    private static <T> T readObject(Scanner scanner, Function<String, T> conv, Predicate<T> isValid, String promptMessage, String errorMessage, boolean canBeEmpty, boolean quiet) {
        while (true) {
            if (!quiet) {
                System.out.print(promptMessage);
            }
            String line = scanner.nextLine();
            if (canBeEmpty && line.isEmpty()) {
                return null;
            }
            try {
                T t = conv.apply(line);
                if (isValid.test(t)) {
                    return t;
                }
            } catch (Exception ignored) {}
            System.out.println(errorMessage);
        }
    }

    private SpaceMarine readMarine(Scanner scanner, boolean quiet) {
        String name = readObject(scanner,
                s -> s,
                s -> !s.isEmpty(),
                "Enter name: ",
                "name can't be empty",
                false,
                quiet);

        double x = readObject(scanner,
                Double::parseDouble,
                d -> true,
                "Enter x coordinate: ",
                "not a valid coordinate",
                false,
                quiet);
        Double y = readObject(scanner,
                Double::parseDouble,
                d -> true,
                "Enter y coordinate: ",
                "not a valid coordinate",
                false,
                quiet);

        Coordinates coordinates = new Coordinates(x, y);

        Date creationDate = new Date();

        Float health = readObject(scanner,
                Float::parseFloat,
                f -> f > 0,
                "Enter health (must be >0): ",
                "not a valid health value",
                false,
                quiet);

        AstartesCategory category = readObject(scanner,
                AstartesCategory::valueOf,
                c -> true,
                "Enter a category (one of [" + Arrays.stream(AstartesCategory.values())
                        .map(AstartesCategory::toString)
                        .collect(Collectors.joining(", ")) + "]) or leave empty: ",
                "not a valid category",
                true,
                quiet);

        Weapon weaponType = readObject(scanner,
                Weapon::valueOf,
                w -> true,
                "Enter a weapong type (one of [" + Arrays.stream(Weapon.values())
                        .map(Weapon::toString)
                        .collect(Collectors.joining(", ")) + "]): ",
                "not a valid weapon type",
                false,
                quiet);

        MeleeWeapon meleeWeapon = readObject(scanner,
                MeleeWeapon::valueOf,
                mw -> true,
                "Enter a melee weapon type (one of [" + Arrays.stream(MeleeWeapon.values())
                        .map(MeleeWeapon::toString)
                        .collect(Collectors.joining(", ")) + "]): ",
                "not a valid melee weapon type",
                false,
                quiet);

        boolean needChapter = readObject(scanner,
                s -> {
                    if (s.equals("y")) {
                        return true;
                    } else if (s.equals("n")) {
                        return false;
                    } else {
                        throw new IllegalArgumentException();
                    }
                },
                nc -> true,
                "Do you want to add a chapter (y/n): ",
                "enter 'y' or 'n'",
                false,
                quiet);
        Chapter chapter = null;
        if (needChapter) {
            String chapterName = readObject(scanner,
                    cn -> cn,
                    s -> !s.isEmpty(),
                    "Enter chapter name: ",
                    "chapter name can't be empty",
                    false,
                    quiet);

            String world = readObject(scanner,
                    w -> w,
                    w -> true,
                    "Enter world name or leave empty: ",
                    "",
                    true,
                    quiet);
            chapter = new Chapter(chapterName, world);
        }
        return new SpaceMarine(++maxid, name, coordinates, creationDate, health,
                category, weaponType, meleeWeapon, chapter);
    }

    private void show() {
        marines.forEach((key, marine) -> {
            printMarine(key, marine);
            System.out.println();
        });
    }

    private void insert(Long key, SpaceMarine marine) {
        marines.put(key, marine);
    }

    private void update(Long id, SpaceMarine marine) {
        marine.setId(id);
        marines.keySet().stream()
                .filter(k -> marines.get(k).getId().equals(id))
                .forEach(k -> marines.put(k, marine));
    }

    private void removeKey(Long key) {
        marines.remove(key);
    }

    private void clear() {
        marines.clear();
    }

    private void removeLower(SpaceMarine marine) {
        marines.keySet().stream()
                .filter(k -> marines.get(k).getHealth() < marine.getHealth())
                .forEach(marines::remove);
    }

    private void replaceIfLower(Long key, SpaceMarine marine) {
        if (marine.getHealth() < marines.get(key).getHealth()) {
            marines.put(key, marine);
        }
    }

    private void removeLowerKey(Long key) {
        marines.keySet().stream()
                .filter(k -> k < key)
                .forEach(marines::remove);
    }

    private void groupCountingByCreationDate() {
        Map<Date, Long> groups = marines.values().stream()
                .collect(Collectors.groupingBy(SpaceMarine::getCreationDate
                        , Collectors.counting()));
        groups.forEach((date, number)
                -> System.out.println(dateFormat.format(date) + ": " + number));
    }

    private void filterGreaterThanCategory(AstartesCategory category) {
        for (Map.Entry<Long, SpaceMarine> e : marines.entrySet()) {
            AstartesCategory marineCat = e.getValue().getCategory();
            if (marineCat!= null && marineCat.ordinal() > category.ordinal()) {
                printMarine(e.getKey(), e.getValue());
                System.out.println();
            }
        }
    }

    private static <A,B,C> Function<A, C> compose(Function<B, C> f, Function<A, B> g) {
        return g.andThen(f);
    }

    private void printAscending() {
        marines.entrySet().stream()
                .sorted(Comparator.comparing(compose(SpaceMarine::getHealth, Map.Entry::getValue)))
                .forEach(e -> {
                    printMarine(e.getKey(), e.getValue());
                    System.out.println();
                });
    }
}
