/**
 * Copyright (C) 2012 - 2014 Xeiam LLC http://xeiam.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xeiam.xchange.bitcoincharts.service.polling;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import si.mazi.rescu.RestProxyFactory;

import com.xeiam.xchange.CachedDataSession;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.NotAvailableFromExchangeException;
import com.xeiam.xchange.bitcoincharts.BitcoinCharts;
import com.xeiam.xchange.bitcoincharts.BitcoinChartsAdapters;
import com.xeiam.xchange.bitcoincharts.BitcoinChartsUtils;
import com.xeiam.xchange.bitcoincharts.dto.marketdata.BitcoinChartsTicker;
import com.xeiam.xchange.currency.Currencies;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.ExchangeInfo;
import com.xeiam.xchange.dto.marketdata.OrderBook;
import com.xeiam.xchange.dto.marketdata.Ticker;
import com.xeiam.xchange.dto.marketdata.Trades;
import com.xeiam.xchange.service.polling.BasePollingExchangeService;
import com.xeiam.xchange.service.polling.PollingMarketDataService;
import com.xeiam.xchange.utils.Assert;

/**
 * @author timmolter
 */
public class BitcoinChartsMarketDataService extends BasePollingExchangeService implements PollingMarketDataService, CachedDataSession {

  private final Logger logger = LoggerFactory.getLogger(BitcoinChartsMarketDataService.class);

  private final BitcoinCharts bitcoinCharts;

  /**
   * time stamps used to pace API calls
   */
  private long tickerRequestTimeStamp = 0L;

  private BitcoinChartsTicker[] cachedBitcoinChartsTickers;

  /**
   * Constructor
   * 
   * @param exchangeSpecification The {@link ExchangeSpecification}
   */
  public BitcoinChartsMarketDataService(ExchangeSpecification exchangeSpecification) {

    super(exchangeSpecification);
    this.bitcoinCharts = RestProxyFactory.createProxy(BitcoinCharts.class, exchangeSpecification.getPlainTextUri());
  }

  @Override
  public long getRefreshRate() {

    return BitcoinChartsUtils.REFRESH_RATE_MILLIS;
  }

  @Override
  public List<CurrencyPair> getExchangeSymbols() {

    return BitcoinChartsUtils.CURRENCY_PAIRS;
  }

  @Override
  public Ticker getTicker(String tradableIdentifier, String currency, Object... args) throws IOException {

    verify(tradableIdentifier, currency);

    // check for pacing violation
    if (tickerRequestTimeStamp == 0L || System.currentTimeMillis() - tickerRequestTimeStamp >= getRefreshRate()) {

      logger.debug("requesting BitcoinCharts tickers");
      tickerRequestTimeStamp = System.currentTimeMillis();

      // Request data
      cachedBitcoinChartsTickers = bitcoinCharts.getMarketData();
    }

    return BitcoinChartsAdapters.adaptTicker(cachedBitcoinChartsTickers, tradableIdentifier);
  }

  @Override
  public OrderBook getOrderBook(String tradableIdentifier, String currency, Object... args) throws IOException {

    throw new NotAvailableFromExchangeException();
  }

  @Override
  public Trades getTrades(String tradableIdentifier, String currency, Object... args) throws IOException {

    throw new NotAvailableFromExchangeException();
  }

  @Override
  public ExchangeInfo getExchangeInfo() throws IOException {

    throw new NotAvailableFromExchangeException();
  }

  /**
   * Verify
   * 
   * @param tradableIdentifier The tradable identifier (e.g. BTC in BTC/USD)
   * @param currency The transaction currency (e.g. USD in BTC/USD)
   */
  private void verify(String tradableIdentifier, String currency) {

    Assert.notNull(tradableIdentifier, "tradableIdentifier cannot be null");
    Assert.isTrue(currency.equals(Currencies.BTC), "Base curreny must be " + Currencies.BTC + " for this exchange");
    Assert.isTrue(BitcoinChartsUtils.isValidCurrencyPair(new CurrencyPair(tradableIdentifier, currency)), "currencyPair is not valid:" + tradableIdentifier + " " + currency);
  }

}
