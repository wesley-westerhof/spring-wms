package graphql;

import base.TokenManager;
import com.coxautodev.graphql.tools.GraphQLMutationResolver;
import entity.*;
import graphql.schema.DataFetchingEnvironment;
import graphql.types.LoginPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import repository.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

@Component
public class Mutation implements GraphQLMutationResolver {

    private UserRepository userRepository;
    private ItemRepository itemRepository;
    private LocationRepository locationRepository;
    private CategoryRepository categoryRepository;
    private TransactionRepository transactionRepository;
    private TransactionRuleRepository transactionRuleRepository;
    private AccountRepository accountRepository;
    private BalanceMutationRepository balanceMutationRepository;
    private BalanceRepository balanceRepository;

    @Autowired
    public Mutation(UserRepository userRepository,
                    ItemRepository itemRepository,
                    LocationRepository locationRepository,
                    CategoryRepository categoryRepository,
                    TransactionRepository transactionRepository,
                    TransactionRuleRepository transactionRuleRepository,
                    AccountRepository accountRepository,
                    BalanceRepository balanceRepository,
                    BalanceMutationRepository balanceMutationRepository) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.locationRepository = locationRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.transactionRuleRepository = transactionRuleRepository;
        this.accountRepository = accountRepository;
        this.balanceRepository = balanceRepository;
        this.balanceMutationRepository = balanceMutationRepository;
    }

    private String idNotFoundMessage(int id, String entity){
        return "No " + entity.toLowerCase() + " found with id " + id + ".";
    }

    public LoginPayload login(String email, String password) {
        User user = userRepository
            .authenticate(email, password)
            .orElse(null);
        if (user == null) throw new GraphQLException("Invalid login");

        String token = TokenManager.generateToken();
        user.setToken(token);
        userRepository.save(user);

        return new LoginPayload(token, user);
    }

    public Item createItem(String name, String code, int recommendedStock, int locationId, DataFetchingEnvironment env) {
        AuthContext.requireAuth(env);

        return itemRepository.save(new Item(
                        name,
                        code,
                        recommendedStock,
                        locationRepository
                            .findById(locationId)
                            .orElseThrow(() -> new GraphQLException(idNotFoundMessage(locationId, Location.class.getSimpleName())))));
    }

    public Location createLocation(String code, int depth, int width, int height, DataFetchingEnvironment env) {
        AuthContext.requireAuth(env);
        
        return locationRepository.save(new Location(code, depth, width, height));
    }

    public Category createCategory(String name, DataFetchingEnvironment env) {
        AuthContext.requireAuth(env);

        return categoryRepository.save(new Category(name));
    }

    public Item itemChangeLocation(int itemId, int locationId, DataFetchingEnvironment env) {
        AuthContext.requireAuth(env);

        Item item = itemRepository
                .findById(itemId)
                .orElseThrow(() -> new GraphQLException(idNotFoundMessage(itemId, Item.class.getSimpleName())));
        Set<Location> locations = item.getLocations();

        Location location = locationRepository
                .findById(locationId)
                .orElseThrow(() -> new GraphQLException(idNotFoundMessage(locationId, Location.class.getSimpleName())));

        locations.add(location);
        return itemRepository.save(item);
    }

    public Item itemAddCategory(int itemId, int categoryId, DataFetchingEnvironment env) {
        AuthContext.requireAuth(env);

        Item item = itemRepository
                .findById(itemId)
                .orElseThrow(() -> new GraphQLException(idNotFoundMessage(itemId, Item.class.getSimpleName())));

        Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new GraphQLException(idNotFoundMessage(categoryId, Category.class.getSimpleName())));

        Set<Category> categories = item.getCategories();
        for (Category cat: categories) {
            if (cat.getId() == categoryId) { throw new GraphQLException("Item (id:" + itemId + ") is already added to this category (id:" + categoryId + ")."); }
        }
        categories.add(category);
        return itemRepository.save(item);
    }

    public Item itemRemoveCategory(int itemId, int categoryId, DataFetchingEnvironment env) {
        AuthContext.requireAuth(env);

        Item item = itemRepository
                .findById(itemId)
                .orElseThrow(() -> new GraphQLException(idNotFoundMessage(itemId, Item.class.getSimpleName())));

        Set<Category> categories = item.getCategories();
        for (Category category : categories) {
            if (category.getId() == categoryId) {
                categories.remove(category);
                return itemRepository.save(item);
            }
        }
        throw new GraphQLException("This item (id:" + itemId + ") is not part of this category (id:" + categoryId + "). Therefore, it can not be removed from it.");
    }

    public Category categoryChangeName(int id, String name, DataFetchingEnvironment env) {
        AuthContext.requireAuth(env);

        Category category = categoryRepository
                .findById(id)
                .orElseThrow(() -> new GraphQLException(idNotFoundMessage(id, Category.class.getSimpleName())));

        category.setName(name);
        return categoryRepository.save(category);
    }

    public Category categoryAddLocation(int categoryId, int locationId, DataFetchingEnvironment env) {
        AuthContext.requireAuth(env);

        Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new GraphQLException(idNotFoundMessage(categoryId, Category.class.getSimpleName())));

        Location location = locationRepository
                .findById(locationId)
                .orElseThrow(() -> new GraphQLException(idNotFoundMessage(locationId, Location.class.getSimpleName())));

        Set<Location> locations = category.getLocations();
        for (Location loc: locations) {
            if (loc.getId() == locationId) { throw new GraphQLException("Location (id:" + locationId+ ") is already added to this category (id:" + categoryId + ")."); }
        }
        locations.add(location);
        return categoryRepository.save(category);
    }

    public Category categoryRemoveLocation(int categoryId, int locationId, DataFetchingEnvironment env) {
        AuthContext.requireAuth(env);

        Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new GraphQLException(idNotFoundMessage(categoryId, Category.class.getSimpleName())));

        Set<Location> locations = category.getLocations();
        for (Location location : locations) {
            if (location.getId() == locationId) {
                locations.remove(location);
                return categoryRepository.save(category);
            }
        }
        throw new GraphQLException("This location (id:" + locationId + ") is not part of this category (id:" + categoryId + "). Therefore, it can not be removed from it.");
    }

    public Boolean deleteItem(int id, DataFetchingEnvironment env) {
        AuthContext.requireAuth(env);

        itemRepository.delete(itemRepository
                .findById(id)
                .orElseThrow(() -> new GraphQLException(idNotFoundMessage(id, Item.class.getSimpleName()))));
        return true;
    }

    public Boolean deleteLocation(int id, DataFetchingEnvironment env) {
        AuthContext.requireAuth(env);

        locationRepository.delete(locationRepository
                .findById(id)
                .orElseThrow(() -> new GraphQLException(idNotFoundMessage(id, Location.class.getSimpleName()))));
        return true;
    }

    public Boolean deleteCategory(int id, DataFetchingEnvironment env) {
        AuthContext.requireAuth(env);

        categoryRepository.delete(categoryRepository
                .findById(id)
                .orElseThrow(() -> new GraphQLException(idNotFoundMessage(id, Category.class.getSimpleName()))));
        return true;
    }

    public User createUser(String firstName, String lastName, String email, String password, DataFetchingEnvironment env) {
        AuthContext.requireAuth(env);

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(password);

        return userRepository.save(user);
    }

    public Account createAccount(String name, DataFetchingEnvironment env) {
        Optional<Account> result = accountRepository.findByName(name);
        if (result.isPresent()) throw new GraphQLException("Account " + name + " already exists.");
        return accountRepository.save(new Account(name));
    }

    public Balance createBalance(int itemId, int accountId, Integer amount, DataFetchingEnvironment env) {
        AuthContext.requireAuth(env);


        Item item = itemRepository
                .findById(itemId)
                .orElseThrow(() -> new GraphQLException(idNotFoundMessage(itemId, Item.class.getSimpleName())));

        Account account = accountRepository
                .findById(accountId)
                .orElseThrow(() -> new GraphQLException(idNotFoundMessage(accountId, Account.class.getSimpleName())));

        if (balanceRepository.findByAccountAndItem(account, item).isPresent())
            throw new GraphQLException("A balance with account (" + account.getName() + ") and item " + item.getName() + " already exists");

        return balanceRepository.save(new Balance(account, item, amount == null ? 0 : amount));
    }

    public Transaction createReservationTransaction(Integer itemId, Integer amount, LocalDate plannedDate, DataFetchingEnvironment env) {
        AuthContext.requireAuth(env);

        Account fromAccount = accountRepository
                .findByName(Account.WAREHOUSE)
                .orElseGet(() -> accountRepository.save(new Account(Account.WAREHOUSE)));
        Account toAccount = accountRepository
                .findByName(Account.IN_USE)
                .orElseGet(() -> accountRepository.save(new Account(Account.IN_USE)));

        Transaction transaction = transactionRepository.save(new Transaction(fromAccount, toAccount));

        if (itemId != null && amount != null)
            addRuleToTransaction(transaction.getId(), itemId, amount, plannedDate, env);

        return transaction;
    }

    public TransactionRule addRuleToTransaction(Integer transactionId, Integer itemId, Integer amount, LocalDate plannedDate, DataFetchingEnvironment env) {
        AuthContext.requireAuth(env);

        Transaction transaction = transactionRepository
                .findById(transactionId)
                .orElseThrow(() -> new GraphQLException(idNotFoundMessage(transactionId, Transaction.class.getSimpleName())));

        if (transaction.getLocked()) throw new GraphQLException("This transaction is locked.");

        Item item = itemRepository
                .findById(itemId)
                .orElseThrow(() -> new GraphQLException(idNotFoundMessage(itemId, Item.class.getSimpleName())));

        return transactionRuleRepository.save(new TransactionRule(amount, transaction, item, plannedDate == null ? LocalDate.now() : plannedDate));
    }

    private boolean safeTransactionCheck(Transaction transaction) {
        Iterable<TransactionRule> transactionRules = transactionRuleRepository.findAllByTransactionId(transaction.getId());

        for (TransactionRule transactionRule:
                transactionRules) {
            Item item = transactionRule.getItem();
            Account fromAccount = transaction.getFrom();
            Balance balance = balanceRepository
                    .findByAccountAndItem(fromAccount, item)
                    .orElseThrow(() -> new GraphQLException("No stock defined for item " + item.getName() + " at " + fromAccount.getName() + "."));

            if (balance.getAmount() < transactionRule.getAmount()) throw new GraphQLException("Not enough stock for item");
        }
        return true;
    }

    private void processBalanceChanges(Transaction transaction) {
        Iterable<TransactionRule> transactionRules = transactionRuleRepository.findAllByTransactionId(transaction.getId());
        for (TransactionRule transactionRule:
                transactionRules) {
            Account fromAccount = transaction.getFrom();
            Item item = transactionRule.getItem();

            Balance balance = balanceRepository
                    .findByAccountAndItem(fromAccount, item)
                    .orElseThrow(() -> new GraphQLException("No stock defined for item " + item.getName() + " at " + fromAccount.getName() + "."));
            balance.setAmount(balance.getAmount() + transactionRule.getAmount() * -1);
            balanceMutationRepository.save(new BalanceMutation(fromAccount, item, transactionRule.getAmount() * -1, "Transaction: " + transaction.getId() + " Rule:" + transactionRule.getId()));
            balanceRepository.save(balance);
            transactionRule.setActualDate(LocalDate.now());
            transactionRuleRepository.save(transactionRule);
        }
    }

    public Transaction executeTransaction(int transactionId, DataFetchingEnvironment env) {
        AuthContext.requireAuth(env);

        Transaction transaction = transactionRepository
                .findById(transactionId)
                .orElseThrow(() -> new GraphQLException(idNotFoundMessage(transactionId, Transaction.class.getSimpleName())));

        if (safeTransactionCheck(transaction)) processBalanceChanges(transaction);

        transaction.setReceivedDate(LocalDate.now());
        transaction.setLocked(true);
        return transactionRepository.save(transaction);
    }
}
